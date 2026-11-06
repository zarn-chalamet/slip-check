package com.slipcheck.backend.adapter.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.service.ChannelNotifier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Minimal Telegram Bot API client (direct HTTP; sendMessage / getFile / file download /
 * getUpdates / answerCallbackQuery). Token comes only from TELEGRAM_BOT_TOKEN and is
 * never logged. Also acts as the TELEGRAM {@link ChannelNotifier} for retry follow-ups
 * and daily summaries (Telegram push is free).
 */
@Component
public class TelegramClient implements ChannelNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final String token;
    private final RestClient api;
    private final RestClient files;
    private final ObjectMapper mapper = new ObjectMapper();

    public TelegramClient(@Value("${telegram.bot-token}") String token) {
        this.token = token == null ? "" : token.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(40_000); // long-polling getUpdates needs a generous read timeout
        this.api = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.telegram.org/bot" + this.token)
                .build();
        this.files = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.telegram.org/file/bot" + this.token)
                .build();
    }

    public boolean isConfigured() {
        return !token.isBlank();
    }

    @Override
    public Channel channel() {
        return Channel.TELEGRAM;
    }

    @Override
    public void send(String externalUserId, String text) {
        sendMessage(parseChatId(externalUserId), text);
    }

    public void sendMessage(long chatId, String text) {
        post("/sendMessage", Map.of("chat_id", chatId, "text", text));
    }

    /** /start and /lang show the three language choices as an inline keyboard. */
    public void sendLanguageKeyboard(long chatId, String prompt) {
        List<Map<String, String>> row = List.of(
                Map.of("text", "English", "callback_data", "lang_en"),
                Map.of("text", "ไทย", "callback_data", "lang_th"),
                Map.of("text", "မြန်မာ", "callback_data", "lang_my"));
        post("/sendMessage", Map.of("chat_id", chatId, "text", prompt,
                "reply_markup", Map.of("inline_keyboard", List.of(row))));
    }

    public void answerCallbackQuery(String callbackQueryId) {
        post("/answerCallbackQuery", Map.of("callback_query_id", callbackQueryId));
    }

    /** Download a file by file_id. Messages handle photo + image documents. */
    public byte[] downloadById(String fileId) {
        JsonNode response = get("/getFile?file_id=" + fileId);
        String path = response.path("result").path("file_path").asString(null);
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("telegram getFile returned no file_path");
        }
        byte[] bytes = files.get().uri("/" + path).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("telegram file download was empty");
        }
        return bytes;
    }

    /** Long-poll updates (dev mode). */
    public JsonNode getUpdates(long offset, int timeoutSeconds) {
        return postForJson("/getUpdates", Map.of("offset", offset, "timeout", timeoutSeconds));
    }

    private void post(String path, Object body) {
        if (!isConfigured()) {
            return;
        }
        try {
            api.post().uri(path).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("telegram API call {} failed: {}", path, e.getMessage());
        }
    }

    private JsonNode postForJson(String path, Object body) {
        try {
            String json = api.post().uri(path).body(body).retrieve().body(String.class);
            return json == null ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception e) {
            log.warn("telegram API call {} failed: {}", path, e.getMessage());
            return mapper.createObjectNode();
        }
    }

    private JsonNode get(String path) {
        String json = api.get().uri(path).retrieve().body(String.class);
        try {
            return json == null ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("telegram response was not JSON", e);
        }
    }

    private static long parseChatId(String externalUserId) {
        try {
            return Long.parseLong(externalUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad telegram chat id");
        }
    }
}
