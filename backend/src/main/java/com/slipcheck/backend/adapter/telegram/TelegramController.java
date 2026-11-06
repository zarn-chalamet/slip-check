package com.slipcheck.backend.adapter.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Telegram webhook (production mode). Authenticity: the secret path embedded in the URL and
 * Telegram's X-Telegram-Bot-Api-Secret-Token header are checked against
 * TELEGRAM_WEBHOOK_SECRET (constant-time). Returns 200 immediately; processing is async.
 */
@RestController
@RequestMapping("/api/v1/webhooks/telegram")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramBotService botService;
    private final String webhookSecret;

    public TelegramController(TelegramBotService botService,
                              @Value("${telegram.webhook-secret:}") String webhookSecret) {
        this.botService = botService;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    @PostMapping("/{secretPath}")
    public ResponseEntity<Void> webhook(@PathVariable String secretPath,
                                        @RequestHeader(value = SECRET_HEADER, required = false) String secretToken,
                                        @RequestBody(required = false) JsonNode update) {
        if (webhookSecret.isBlank()) {
            // Dev convenience: without TELEGRAM_WEBHOOK_SECRET the endpoint is open.
            log.debug("telegram webhook reached without a configured secret");
        } else if (!constantTimeEquals(webhookSecret, secretPath)
                && !constantTimeEquals(webhookSecret, secretToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            botService.handleUpdate(update);
        } catch (Exception e) {
            log.warn("telegram update handling failed: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
