package com.slipcheck.backend.adapter.line;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.service.ChannelNotifier;

import java.util.List;
import java.util.Map;

/**
 * Minimal LINE Messaging API client.
 *
 * - Replies use the reply token (free tier friendly, single use, short-lived).
 * - Push messages are DISABLED by default (line.push-enabled=false); the ChannelNotifier
 *   implementation no-ops unless explicitly enabled — reply tokens are the default path.
 * - Image bytes come from the message-content API. The access token is never logged.
 */
@Component
public class LineClient implements ChannelNotifier {

    private static final Logger log = LoggerFactory.getLogger(LineClient.class);

    private final String accessToken;
    private final boolean pushEnabled;
    private final RestClient messageApi;
    private final RestClient contentApi;

    public LineClient(@Value("${line.channel-access-token:}") String accessToken,
                      @Value("${line.push-enabled:false}") boolean pushEnabled) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.pushEnabled = pushEnabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.messageApi = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.line.me/v2/bot/message")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .build();
        this.contentApi = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api-data.line.me/v2/bot/message")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .build();
    }

    public boolean isConfigured() {
        return !accessToken.isBlank();
    }

    @Override
    public Channel channel() {
        return Channel.LINE;
    }

    @Override
    public void send(String externalUserId, String text) {
        if (!isConfigured()) {
            log.warn("LINE client is not configured with a valid channel access token");
            return;
        }
        if (!pushEnabled) {
            log.warn("LINE push message skipped for user {} because line.push-enabled is false", externalUserId);
            return;
        }
        post("/push", Map.of("to", externalUserId, "messages", List.of(textMessage(text))));
    }

    public byte[] downloadContent(String messageId) {
        byte[] bytes = contentApi.get().uri("/{id}/content", messageId)
                .retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("LINE content download was empty");
        }
        return bytes;
    }

    public void replyText(String replyToken, String text) {
        reply(replyToken, List.of(textMessage(text)));
    }

    /** Language choice as quick replies (each sends "/lang <code>"). */
    public void replyLanguageQuickReplies(String replyToken, String prompt) {
        Map<String, Object> message = Map.of(
                "type", "text",
                "text", prompt,
                "quickReply", Map.of("items", List.of(
                        quickReplyItem("English", "/lang en"),
                        quickReplyItem("ไทย", "/lang th"),
                        quickReplyItem("မြန်မာ", "/lang my"))));
        reply(replyToken, List.of(message));
    }

    private void reply(String replyToken, List<Map<String, Object>> messages) {
        post("/reply", Map.of("replyToken", replyToken, "messages", messages));
    }

    private void post(String path, Object body) {
        if (!isConfigured()) {
            return;
        }
        try {
            messageApi.post().uri(path).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("LINE API call {} failed: {}", path, e.getMessage());
        }
    }

    private static Map<String, Object> textMessage(String text) {
        return Map.of("type", "text", "text", text);
    }

    private static Map<String, Object> quickReplyItem(String label, String sendText) {
        return Map.of("type", "action",
                "action", Map.of("type", "message", "label", label, "text", sendText));
    }
}
