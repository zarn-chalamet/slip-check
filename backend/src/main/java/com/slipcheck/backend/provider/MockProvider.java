package com.slipcheck.backend.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.core.model.ProviderException;
import com.slipcheck.backend.core.model.VerifiedSlip;
import com.slipcheck.backend.core.model.ProviderException.ProviderError;

import java.math.BigDecimal;
import java.time.Instant;

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

        // --- FREE LOCAL SCANNER HACK ---
        // If it starts with 000201, it is a real Thai bank QR code.
        // We bypass the internet and pretend it verified successfully so you can test your UI for free.
        if (qrPayload.startsWith("000201")) {
            return new VerifiedSlip(
                    "KBANK", 
                    "LOCAL-TEST-" + System.currentTimeMillis(),
                    new BigDecimal("0.00"), // We can't know the exact amount without paying for an API
                    Instant.now(),
                    "Real QR Scanned Locally",
                    "xxx-x-x0000-x",
                    "{\"note\": \"Verified locally via MockProvider (No API key used)\"}"
            );
        }

        // --- STANDARD MOCK LOGIC ---
        if (qrPayload.startsWith("MOCK_OK_")) {
            String[] parts = qrPayload.trim().split("_");
            String amountRaw = parts.length > 2 ? parts[2] : "0";
            String suffix = parts.length > 3 ? parts[3] : "0000";
            return new VerifiedSlip(
                    "KBANK",
                    "MOCKREF-" + amountRaw + "-" + suffix,
                    new BigDecimal(amountRaw),
                    Instant.now(),
                    "Mock User",
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