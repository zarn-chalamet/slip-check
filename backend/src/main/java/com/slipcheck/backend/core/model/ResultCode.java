package com.slipcheck.backend.core.model;

/**
 * All verdict codes the system can produce (spec section 13.1).
 * The core returns codes only; {@code MessageFormatter} turns them into text.
 */
public enum ResultCode {
    /** Real transaction, to the seller's account, first use, amount OK (or not checked). */
    VERIFIED,
    /** Real and to the seller, but the amount differs from the caption amount. Goes to review. */
    AMOUNT_MISMATCH,
    /** Real, but the receiver could not be confirmed (masked data insufficient). Goes to review. */
    RECEIVER_UNCERTAIN,
    /** Transaction already claimed by this seller. */
    DUPLICATE_SLIP,
    /** Real transaction, definitely not to a registered account. Never claimed. */
    WRONG_RECEIVER,
    /** No QR code could be read. */
    UNREADABLE_QR,
    /** Provider has no matching transaction. */
    SLIP_NOT_FOUND,
    /** Bank is still processing; auto-retry scheduled. */
    BANK_PENDING,
    /** Verification service down or timed out; auto-retry scheduled. */
    PROVIDER_UNAVAILABLE,
    /** Provider quota reached. */
    QUOTA_EXCEEDED,
    /** Not an image, unsupported type or too large. */
    INVALID_IMAGE,
    /** Chat not linked to a seller account. */
    NOT_LINKED
}
