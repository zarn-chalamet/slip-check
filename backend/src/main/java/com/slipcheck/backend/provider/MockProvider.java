package com.slipcheck.backend.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.core.model.ProviderException;
import com.slipcheck.backend.core.model.VerifiedSlip;
import com.slipcheck.backend.core.model.ProviderException.ProviderError;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Deterministic provider for development and tests, driven by magic payload prefixes
 * (docs/provider-notes.md):
 *
 *   MOCK_OK_<amountBaht>_<suffix>   -> a real-looking slip; receiver masked "xxx<suffix>xx"
 *   MOCK_NOT_FOUND / MOCK_PENDING / MOCK_QUOTA / MOCK_DOWN -> the matching error
 *
 * transRef is deterministic ("MOCKREF-<amount>-<suffix>") so the duplicate guarantee can
 * be exercised: two submissions of the same mock slip hit the same claimed transaction.
 */
@Component
@ConditionalOnProperty(name = "provider.name", havingValue = "mock", matchIfMissing = true)
public class MockProvider implements SlipVerificationProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public VerifiedSlip verifyByPayload(String qrPayload) throws ProviderException {
        if (qrPayload == null || qrPayload.isBlank()) {
            throw new ProviderException(ProviderError.INVALID_PAYLOAD, "empty payload");
        }
        if (qrPayload.startsWith("MOCK_OK_")) {
            String[] parts = qrPayload.trim().split("_");
            if (parts.length < 4) {
                throw new ProviderException(ProviderError.INVALID_PAYLOAD,
                        "expected MOCK_OK_<amountBaht>_<suffix>");
            }
            String amountRaw = parts[2];
            String suffix = parts[3];
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountRaw);
            } catch (NumberFormatException e) {
                throw new ProviderException(ProviderError.INVALID_PAYLOAD, "bad amount: " + amountRaw);
            }
            return new VerifiedSlip(
                    "KBANK",
                    "MOCKREF-" + amountRaw + "-" + suffix,
                    amount,
                    Instant.now(),
                    "Sompong J.",
                    "xxx" + suffix + "xx",
                    "{\"mock\":true}");
        }
        
        switch (qrPayload.trim()) {
            case "MOCK_NOT_FOUND" -> throw new ProviderException(ProviderError.NOT_FOUND, "no matching transfer");
            case "MOCK_PENDING" -> throw new ProviderException(ProviderError.PENDING, "bank still processing");
            case "MOCK_QUOTA" -> throw new ProviderException(ProviderError.QUOTA, "monthly quota exceeded");
            case "MOCK_DOWN" -> throw new ProviderException(ProviderError.UNAVAILABLE, "service unavailable");
            default -> throw new ProviderException(ProviderError.NOT_FOUND, "no matching transfer");
        }
    }
}
