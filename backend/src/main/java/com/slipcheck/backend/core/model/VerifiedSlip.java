package com.slipcheck.backend.core.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The provider's record of the real transaction behind a slip.
 * Receiver fields stay masked exactly as the provider returns them.
 */
public record VerifiedSlip(
        String sendingBank,
        String transRef,
        BigDecimal amountBaht,
        Instant transferredAt,
        String receiverNameMasked,
        String receiverAccountMasked,
        String rawJsonTrimmed) {
}
