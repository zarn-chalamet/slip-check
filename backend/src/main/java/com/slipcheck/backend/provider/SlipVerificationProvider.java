package com.slipcheck.backend.provider;

import com.slipcheck.backend.core.model.ProviderException;
import com.slipcheck.backend.core.model.VerifiedSlip;

/**
 * Abstraction over a slip-verification provider. Implementations: {@code MockProvider}
 * (dev/tests) and one real provider added in Phase 4 after its API is verified against
 * current official docs (see docs/provider-notes.md).
 */
public interface SlipVerificationProvider {

    /** Short name persisted on submissions for audit, e.g. "mock". */
    String name();

    /**
     * Verify a slip by the payload decoded from its QR code.
     *
     * @throws ProviderException NOT_FOUND (no such transaction), PENDING (bank still
     *                           processing), QUOTA (monthly limit), UNAVAILABLE
     *                           (timeout/5xx — retryable), INVALID_PAYLOAD
     */
    VerifiedSlip verifyByPayload(String qrPayload) throws ProviderException;
}
