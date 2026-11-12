package com.slipcheck.backend.security;

import org.springframework.stereotype.Service;

import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.audit.AuditService.ActorType;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.rules.CaptionParser;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.entity.LinkCodeEntity;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.LinkCodeRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * One-time 6-digit chat link codes (spec F1 step 4).
 * Codes are stored hashed (SHA-256), valid 10 minutes, single use; a chat is locked out
 * for the window after 5 failed attempts.
 */
@Service
public class LinkCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public record CreatedCode(String code, Instant expiresAt) {
    }

    private final LinkCodeRepository linkCodes;
    private final ChannelIdentityRepository identities;
    private final SellerRepository sellers;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    /** Failed-attempt windows per chat identity. */
    private final Map<String, ConcurrentLinkedDeque<Instant>> failedAttempts = new ConcurrentHashMap<>();

    public LinkCodeService(LinkCodeRepository linkCodes, ChannelIdentityRepository identities,
                           SellerRepository sellers, AuditService audit) {
        this.linkCodes = linkCodes;
        this.identities = identities;
        this.sellers = sellers;
        this.audit = audit;
    }

    public CreatedCode create(UUID sellerId) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        linkCodes.save(new LinkCodeEntity(sellerId, sha256Hex(code), Instant.now().plus(CODE_TTL)));
        return new CreatedCode(code, Instant.now().plus(CODE_TTL));
    }

    /**
     * Redeem a code from a chat. On success the (channel, externalUserId) identity is linked
     * to the seller that created the code (creating or re-pointing the identity).
     */
    public Optional<ChannelIdentityEntity> redeem(Channel channel, String externalUserId, String rawCode) {
        String attemptKey = channel.name() + ":" + externalUserId;
        if (isLockedOut(attemptKey)) {
            return Optional.empty();
        }
        String normalised = rawCode == null ? "" : CaptionParser.normalizeDigits(rawCode.trim());
        Optional<LinkCodeEntity> codeOpt = linkCodes.findByCodeHashAndUsedAtIsNull(sha256Hex(normalised));

        if (codeOpt.isEmpty() || codeOpt.get().getExpiresAt().isBefore(Instant.now())) {
            recordFailedAttempt(attemptKey);
            return Optional.empty();
        }

        LinkCodeEntity code = codeOpt.get();
        code.markUsed();
        linkCodes.save(code);

        SellerEntity seller = sellers.findById(code.getSellerId()).orElseThrow();
        ChannelIdentityEntity identity = identities
                .findByChannelAndExternalUserId(channel.name(), externalUserId)
                .orElseGet(() -> new ChannelIdentityEntity(
                        seller.getId(), channel.name(), externalUserId, seller.getLanguage()));
        identity.setSellerId(seller.getId());
        identity.setLanguage(seller.getLanguage());
        identity.setLinkedAt(Instant.now());
        identities.save(identity);

        failedAttempts.remove(attemptKey);
        audit.record(seller.getId(), ActorType.CHANNEL, channel.name() + ":" + externalUserId,
                "CHAT_LINKED", "channel_identity", identity.getId().toString(),
                Map.of("channel", channel.name()));
        return Optional.of(identity);
    }

    private boolean isLockedOut(String key) {
        ConcurrentLinkedDeque<Instant> deque = failedAttempts.get(key);
        if (deque == null) {
            return false;
        }
        Instant cutoff = Instant.now().minus(CODE_TTL);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
        return deque.size() >= MAX_FAILED_ATTEMPTS;
    }

    private void recordFailedAttempt(String key) {
        failedAttempts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(Instant.now());
        if (failedAttempts.size() > 10_000) {
            failedAttempts.clear(); // crude bound; windows are short
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
