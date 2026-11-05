package com.slipcheck.backend.core.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QR payloads of submissions awaiting a retry, kept in memory only (ADR-2).
 * Privacy rules forbid persisting QR payloads; if the process restarts, pending
 * submissions are finalised with their last code and the seller is asked to resend.
 */
@Component
public class PendingPayloadCache {

    private static final long TTL_SECONDS = 30 * 60; // longer than the maximum retry window

    private record Entry(String payload, Instant expiresAt) {
    }

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public void put(UUID submissionId, String payload) {
        evictExpired();
        cache.put(submissionId, new Entry(payload, Instant.now().plusSeconds(TTL_SECONDS)));
    }

    public Optional<String> get(UUID submissionId) {
        Entry entry = cache.get(submissionId);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(submissionId);
            return Optional.empty();
        }
        return Optional.of(entry.payload());
    }

    public void remove(UUID submissionId) {
        cache.remove(submissionId);
    }

    private void evictExpired() {
        if (cache.size() > 512) { // cheap bound to keep memory flat under load
            Instant now = Instant.now();
            cache.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        }
    }
}
