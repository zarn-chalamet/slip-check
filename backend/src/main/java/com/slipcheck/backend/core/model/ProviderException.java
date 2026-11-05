package com.slipcheck.backend.core.model;

/** Failures a {@code SlipVerificationProvider} can signal. */
public class ProviderException extends Exception {

    public enum ProviderError {
        NOT_FOUND, PENDING, QUOTA, UNAVAILABLE, INVALID_PAYLOAD
    }

    private final ProviderError error;

    public ProviderException(ProviderError error, String message) {
        super(message);
        this.error = error;
    }

    public ProviderError error() {
        return error;
    }
}