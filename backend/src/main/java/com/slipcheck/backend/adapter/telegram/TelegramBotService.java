package com.slipcheck.backend.adapter.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.core.model.SlipSubmissionRequest;
import com.slipcheck.backend.core.service.SlipService;
import com.slipcheck.backend.core.service.SummaryService;
import com.slipcheck.backend.core.service.SummaryService.DailySummary;
import com.slipcheck.backend.i18n.MessageFormatter;
import com.slipcheck.backend.i18n.MessageKeys;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.security.LinkCodeService;

import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * All Telegram message handling. Translates updates into core calls — no business rules.
 * Image work is handed to the bounded executor so webhooks/polling never block.
 */
@Service
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final Set<String> LANGUAGES = Set.of("en", "th", "my");

    private final TelegramClient client;
    private final SlipService slipService;
    private final LinkCodeService linkCodes;
    private final ChannelIdentityRepository identities;
    private final SellerRepository sellers;
    private final MessageFormatter formatter;
    private final SummaryService summaryService;
    private final Executor executor;

    public TelegramBotService(TelegramClient client, SlipService slipService,
                              LinkCodeService linkCodes, ChannelIdentityRepository identities,
                              SellerRepository sellers, MessageFormatter formatter,
                              SummaryService summaryService,
                              @Qualifier("slipExecutor") Executor executor) {
        this.client = client;
        this.slipService = slipService;
        this.linkCodes = linkCodes;
        this.identities = identities;
        this.sellers = sellers;
        this.formatter = formatter;
        this.summaryService = summaryService;
        this.executor = executor;
    }

    public void handleUpdate(JsonNode update) {
        if (update == null) {
            return;
        }
        JsonNode callback = update.get("callback_query");
        if (callback != null) {
            handleCallback(callback);
            return;
        }
        JsonNode message = update.has("message") ? update.get("message") : update.get("edited_message");
        if (message == null) {
            return;
        }
        long chatId = message.path("chat").path("id").asLong();

        JsonNode text = message.get("text");
        if (text != null && !text.asString().isBlank()) {
            handleCommand(chatId, text.asString().trim());
            return;
        }

        String fileId = extractImageFileId(message);
        if (fileId == null) {
            return; // unsupported content: silently ignored
        }
        String caption = message.path("caption").isString() ? message.get("caption").asString() : null;
        String externalMessageId = "tg:" + chatId + ":" + message.path("message_id").asLong();
        executor.execute(() -> processImage(chatId, externalMessageId, fileId, caption));
    }

    // ------------------------------------------------------------------ images

    /** Photos arrive as several compressed sizes; take the largest. Documents keep quality. */
    private String extractImageFileId(JsonNode message) {
        JsonNode photo = message.get("photo");
        if (photo != null && photo.isArray() && !photo.isEmpty()) {
            JsonNode largest = photo.get(photo.size() - 1);
            String fileId = largest.path("file_id").asString(null);
            if (fileId != null) {
                return fileId;
            }
        }
        JsonNode document = message.get("document");
        if (document != null) {
            String mime = document.path("mime_type").asString("");
            if (mime.startsWith("image/")) {
                return document.path("file_id").asString(null);
            }
        }
        return null;
    }

    private void processImage(long chatId, String externalMessageId, String fileId, String caption) {
        String chatIdStr = String.valueOf(chatId);
        String language = languageOf(chatIdStr);
        try {
            byte[] image = client.downloadById(fileId);
            SlipResult result = slipService.process(new SlipSubmissionRequest(
                    Channel.TELEGRAM, chatIdStr, externalMessageId, image, caption, language));
            client.sendMessage(chatId, formatter.forResult(result, language, zoneOf(chatIdStr)));
        } catch (Exception e) {
            log.warn("telegram slip processing failed for {}", externalMessageId, e);
            client.sendMessage(chatId, formatter.format(MessageKeys.SLIP_PROVIDER_UNAVAILABLE, language));
        }
    }

    // ------------------------------------------------------------------ callbacks & commands

    private void handleCallback(JsonNode callback) {
        String id = callback.path("id").asString();
        client.answerCallbackQuery(id);
        String data = callback.path("data").asString("");
        long chatId = callback.path("message").path("chat").path("id").asLong();
        if (chatId == 0) {
            chatId = callback.path("from").path("id").asLong();
        }
        if (data.startsWith("lang_")) {
            setLanguage(chatId, data.substring("lang_".length()));
        }
    }

    private void handleCommand(long chatId, String text) {
        String chatIdStr = String.valueOf(chatId);
        String lower = text.toLowerCase();
        if (lower.startsWith("/start")) {
            client.sendLanguageKeyboard(chatId, formatter.format(MessageKeys.CHAT_LANGUAGE_PROMPT, languageOf(chatIdStr)));
        } else if (lower.startsWith("/link")) {
            String[] parts = text.split("\\s+");
            String code = parts.length > 1 ? parts[1] : "";
            link(chatId, code);
        } else if (lower.startsWith("/lang")) {
            String[] parts = text.split("\\s+");
            if (parts.length > 1 && LANGUAGES.contains(parts[1].toLowerCase())) {
                setLanguage(chatId, parts[1].toLowerCase());
            } else {
                client.sendLanguageKeyboard(chatId, formatter.format(MessageKeys.CHAT_LANGUAGE_PROMPT, languageOf(chatIdStr)));
            }
        } else if (lower.startsWith("/summary")) {
            sendSummary(chatId);
        } else {
            client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_HELP, languageOf(chatIdStr)));
        }
    }

    private void link(long chatId, String code) {
        String chatIdStr = String.valueOf(chatId);
        Optional<ChannelIdentityEntity> linked = linkCodes.redeem(Channel.TELEGRAM, chatIdStr, code);
        if (linked.isEmpty()) {
            client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_LINK_FAILED, languageOf(chatIdStr)));
            return;
        }
        ChannelIdentityEntity identity = linked.get();
        String name = sellers.findById(identity.getSellerId())
                .map(s -> s.getDisplayName()).orElse("your account");
        client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_LINKED, identity.getLanguage(), name));
    }

    private void setLanguage(long chatId, String language) {
        String chatIdStr = String.valueOf(chatId);
        if (!LANGUAGES.contains(language)) {
            return;
        }
        Optional<ChannelIdentityEntity> identity =
                identities.findByChannelAndExternalUserId(Channel.TELEGRAM.name(), chatIdStr);
        if (identity.isEmpty()) {
            client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_NOT_LINKED, "en"));
            return;
        }
        ChannelIdentityEntity entity = identity.get();
        entity.setLanguage(language);
        identities.save(entity);
        client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_LANGUAGE_SET, language));
    }

    private void sendSummary(long chatId) {
        String chatIdStr = String.valueOf(chatId);
        Optional<ChannelIdentityEntity> identity =
                identities.findByChannelAndExternalUserId(Channel.TELEGRAM.name(), chatIdStr);
        if (identity.isEmpty()) {
            client.sendMessage(chatId, formatter.format(MessageKeys.CHAT_NOT_LINKED, "en"));
            return;
        }
        ChannelIdentityEntity entity = identity.get();
        ZoneId zone = summaryService.sellerZone(entity.getSellerId());
        DailySummary summary = summaryService.compile(entity.getSellerId(), LocalDate.now(zone));
        client.sendMessage(chatId, formatter.dailySummary(summary.date(), summary.verified(),
                summary.totalBaht(), summary.review(), summary.duplicates(), summary.rejected(),
                entity.getLanguage()));
    }

    // ------------------------------------------------------------------ helpers

    private String languageOf(String chatId) {
        return identities.findByChannelAndExternalUserId(Channel.TELEGRAM.name(), chatId)
                .map(ChannelIdentityEntity::getLanguage).orElse("en");
    }

    private ZoneId zoneOf(String chatId) {
        return identities.findByChannelAndExternalUserId(Channel.TELEGRAM.name(), chatId)
                .map(ChannelIdentityEntity::getSellerId)
                .map(summaryService::sellerZone)
                .orElse(ZoneId.of("Asia/Bangkok"));
    }
}
