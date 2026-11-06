package com.slipcheck.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.persistence.repo.SlipSubmissionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * PDPA data minimisation: trimmed provider responses older than
 * slipcheck.retention.provider-response-days (default 30) are wiped nightly.
 * Extracted fields (amount, bank, masked receiver) stay for the seller's history.
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final SlipSubmissionRepository submissions;
    private final SlipCheckProperties properties;

    public RetentionJob(SlipSubmissionRepository submissions, SlipCheckProperties properties) {
        this.submissions = submissions;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeProviderResponses() {
        Instant cutoff = Instant.now().minus(properties.retention().providerResponseDays(), ChronoUnit.DAYS);
        int cleared = submissions.clearProviderResponsesOlderThan(cutoff);
        if (cleared > 0) {
            log.info("retention: wiped provider responses on {} submissions", cleared);
        }
    }
}
