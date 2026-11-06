package com.slipcheck.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.core.service.SlipService;

/**
 * Every minute, re-process submissions scheduled for retry (BANK_PENDING /
 * PROVIDER_UNAVAILABLE), with the configured backoff. Final results produce a follow-up
 * message on channels allowed by slipcheck.retry.followup-channels.
 */
@Component
public class RetryJob {

    private static final Logger log = LoggerFactory.getLogger(RetryJob.class);

    private final SlipService slipService;

    public RetryJob(SlipService slipService) {
        this.slipService = slipService;
    }

    @Scheduled(initialDelay = 15_000, fixedDelay = 60_000)
    public void run() {
        try {
            slipService.processDueRetries();
        } catch (Exception e) {
            log.warn("retry sweep failed", e);
        }
    }
}
