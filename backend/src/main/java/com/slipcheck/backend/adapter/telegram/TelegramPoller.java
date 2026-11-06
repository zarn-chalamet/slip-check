package com.slipcheck.backend.adapter.telegram;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Development mode: long polling via getUpdates, no public URL needed
 * (telegram.mode=polling, the default). Production uses the webhook instead.
 */
@Component
@ConditionalOnProperty(name = "telegram.mode", havingValue = "polling", matchIfMissing = true)
public class TelegramPoller {

    private static final Logger log = LoggerFactory.getLogger(TelegramPoller.class);

    private final TelegramClient client;
    private final TelegramBotService botService;
    private volatile boolean running = true;
    private Thread thread;

    public TelegramPoller(TelegramClient client, TelegramBotService botService) {
        this.client = client;
        this.botService = botService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!client.isConfigured()) {
            log.info("telegram poller idle: TELEGRAM_BOT_TOKEN not set");
            return;
        }
        thread = new Thread(this::pollLoop, "telegram-poller");
        thread.setDaemon(true);
        thread.start();
        log.info("telegram long polling started (dev mode)");
    }

    private void pollLoop() {
        long offset = 0;
        while (running) {
            try {
                JsonNode updates = client.getUpdates(offset, 25).path("result");
                if (updates.isArray()) {
                    for (JsonNode update : updates) {
                        offset = Math.max(offset, update.path("update_id").asLong() + 1);
                        try {
                            botService.handleUpdate(update);
                        } catch (Exception e) {
                            log.warn("telegram update handling failed", e);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("telegram polling error: {}", e.getMessage());
                sleep(5_000);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
