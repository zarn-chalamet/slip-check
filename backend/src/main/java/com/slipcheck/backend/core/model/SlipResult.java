package com.slipcheck.backend.core.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Core pipeline output. Carries a code plus the data needed to format any message;
 * contains no user-facing text.
 *
 * @param firstUsedAt    for DUPLICATE_SLIP: when the transaction was first claimed
 * @param retryScheduled true when BANK_PENDING / PROVIDER_UNAVAILABLE will be retried
 */
public record SlipResult(
        ResultCode code,
        UUID submissionId,
        VerifiedSlip slip,
        BigDecimal expectedAmountBaht,
        String label,
        Instant firstUsedAt,
        boolean retryScheduled) {
}
