package com.slipcheck.backend.adapter.line;

import tools.jackson.databind.JsonNode;

import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.core.model.SlipSubmissionRequest;
import com.slipcheck.backend.core.service.SlipService;
import com.slipcheck.backend.core.service.SummaryService;
import com.slipcheck.backend.core.service.SummaryService.DailySummary;
import com.slipcheck.backend.i18n.MessageFormatter;
import com.slipcheck.backend.i18n.MessageKeys;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.security.LinkCodeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * All LINE event handling. Translates events into core calls — no business rules.
 * Replies always use the reply token; image work runs on the bounded executor so the
 * webhook returns 200 immediately.
 */
@Service
public class LineBotService {

    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);
    private static final Set<String> LANGUAGES = Set.of("en", "th", "my");

    private final LineClient client;
    private final SlipService slipService;
    private final LinkCodeService linkCodes;
    private final ChannelIdentityRepository identities;
    private final SellerRepository sellers;
    private final MessageFormatter formatter;
    private final SummaryService summaryService;
    private final Executor executor;

    public LineBotService(LineClient client, SlipService slipService, LinkCodeService linkCodes,
                          ChannelIdentityRepository identities, SellerRepository sellers,
                          MessageFormatter formatter, SummaryService summaryService,
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

    public void handleEvents(JsonNode root) {
        JsonNode events = root == null ? null : root.get("events");
        if (events == null || !events.isArray()) {
            return;
        }
        for (JsonNode event : events) {
            String type = event.path("type").asString("");
            String replyToken = event.path("replyToken").asString(null);
            String userId = event.path("source").path("userId").asString("");
            switch (type) {
                case "follow" -> {
                    if (replyToken != null) {
                        client.replyLanguageQuickReplies(replyToken,
                                formatter.format(MessageKeys.CHAT_LANGUAGE_PROMPT, "en"));
                    }
                }
                case "message" -> handleMessage(event.path("message"), replyToken, userId);
                default -> {
                    // unhandled event types are acknowledged with 200 and ignored
                }
            }
        }
    }

    private void handleMessage(JsonNode message, String replyToken, String userId) {
        String messageType = message.path("type").asString("");
        if ("image".equals(messageType) && replyToken != null) {
            String messageId = message.path("id").asString();
            executor.execute(() -> processImage(replyToken, userId, messageId));
        } else if ("text".equals(messageType) && replyToken != null) {
            handleCommand(replyToken, userId, message.path("text").asString("").trim());
        }
    }

    private void processImage(String replyToken, String userId, String messageId) {
        String language = languageOf(userId);
        try {
            byte[] image = client.downloadContent(messageId);
            SlipResult result = slipService.process(new SlipSubmissionRequest(
                    Channel.LINE, userId, "line:" + messageId, image, null, language));
            client.replyText(replyToken, formatter.forResult(result, language, zoneOf(userId)));
        } catch (Exception e) {
            log.warn("LINE slip processing failed for message {}", messageId, e);
            client.replyText(replyToken, formatter.format(MessageKeys.SLIP_PROVIDER_UNAVAILABLE, language));
        }
    }

    private void handleCommand(String replyToken, String userId, String text) {
        String lower = text.toLowerCase();
        if (lower.startsWith("/start")) {
            client.replyText(replyToken, formatter.format(MessageKeys.CHAT_WELCOME, languageOf(userId)));
        } else if (lower.startsWith("/link")) {
            String[] parts = text.split("\\s+");
            String code = parts.length > 1 ? parts[1] : "";
            Optional<ChannelIdentityEntity> linked = linkCodes.redeem(Channel.LINE, userId, code);
            if (linked.isEmpty()) {
                client.replyText(replyToken,
                        formatter.format(MessageKeys.CHAT_LINK_FAILED, languageOf(userId)));
            } else {
                ChannelIdentityEntity identity = linked.get();
                String name = sellers.findById(identity.getSellerId())
                        .map(SellerEntity::getDisplayName).orElse("your account");
                
                // Sends response in the seller's website language
                client.replyText(replyToken,
                        formatter.format(MessageKeys.CHAT_LINKED, identity.getLanguage(), name));
            }
        } else if (lower.startsWith("/lang")) {
            String[] parts = text.split("\\s+");
            if (parts.length > 1 && LANGUAGES.contains(parts[1].toLowerCase())) {
                setLanguage(replyToken, userId, parts[1].toLowerCase());
            } else {
                client.replyLanguageQuickReplies(replyToken,
                        formatter.format(MessageKeys.CHAT_LANGUAGE_PROMPT, languageOf(userId)));
            }
        } else if (lower.startsWith("/summary")) {
            Optional<ChannelIdentityEntity> identity =
                    identities.findByChannelAndExternalUserId(Channel.LINE.name(), userId);
            if (identity.isEmpty()) {
                client.replyText(replyToken, formatter.format(MessageKeys.CHAT_NOT_LINKED, "en"));
                return;
            }
            ZoneId zone = summaryService.sellerZone(identity.get().getSellerId());
            DailySummary summary = summaryService.compile(identity.get().getSellerId(), LocalDate.now(zone));
            client.replyText(replyToken, formatter.dailySummary(summary.date(), summary.verified(),
                    summary.totalBaht(), summary.review(), summary.duplicates(), summary.rejected(),
                    identity.get().getLanguage()));
        } else {
            client.replyText(replyToken, formatter.format(MessageKeys.CHAT_HELP, languageOf(userId)));
        }
    }

    private void setLanguage(String replyToken, String userId, String language) {
        Optional<ChannelIdentityEntity> identity =
                identities.findByChannelAndExternalUserId(Channel.LINE.name(), userId);
        if (identity.isEmpty()) {
            client.replyText(replyToken, formatter.format(MessageKeys.CHAT_NOT_LINKED, "en"));
            return;
        }
        ChannelIdentityEntity entity = identity.get();
        entity.setLanguage(language);
        identities.save(entity);
        client.replyText(replyToken, formatter.format(MessageKeys.CHAT_LANGUAGE_SET, language));
    }

    private String languageOf(String userId) {
        return identities.findByChannelAndExternalUserId(Channel.LINE.name(), userId)
                .map(ChannelIdentityEntity::getLanguage).orElse("en");
    }

    private ZoneId zoneOf(String userId) {
        return identities.findByChannelAndExternalUserId(Channel.LINE.name(), userId)
                .map(ChannelIdentityEntity::getSellerId)
                .map(summaryService::sellerZone)
                .orElse(ZoneId.of("Asia/Bangkok"));
    }
}
