package com.slipcheck.backend.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.audit.AuditService.ActorType;
import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.model.ProviderException;
import com.slipcheck.backend.core.model.ResultCode;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.core.model.SlipSubmissionRequest;
import com.slipcheck.backend.core.model.VerifiedSlip;
import com.slipcheck.backend.core.qr.QrDecoder;
import com.slipcheck.backend.core.rules.CaptionParser;
import com.slipcheck.backend.core.rules.ReceiverMatcher;
import com.slipcheck.backend.core.rules.CaptionParser.CaptionResult;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.entity.ClaimedTransactionEntity;
import com.slipcheck.backend.persistence.entity.SlipSubmissionEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.ClaimedTransactionRepository;
import com.slipcheck.backend.persistence.repo.SellerBankAccountRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.persistence.repo.SlipSubmissionRepository;
import com.slipcheck.backend.provider.SlipVerificationProvider;
import com.slipcheck.backend.security.CryptoService;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The channel-agnostic slip pipeline (spec section 12).
 *
 * 1. resolve seller via channel identity   2. idempotent insert (unique message id)
 * 3. image validation (magic bytes + size) 4. sha-256          5. QR decode (ZXing)
 * 6. provider verification                 7. receiver match   8. claim (unique constraint!)
 * 9. amount check                          10. persist + audit
 *
 * Duplicate protection is the unique key on claimed_transactions — see ADR-4.
 * Slip images and QR payloads are never persisted (payloads for pending retries stay in
 * {@link PendingPayloadCache}, memory only — ADR-2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlipService {

    private static final int PROVIDER_RESPONSE_MAX_CHARS = 4000;

    private final SlipSubmissionRepository submissions;
    private final ClaimedTransactionRepository claims;
    private final ChannelIdentityRepository identities;
    private final SellerBankAccountRepository accounts;
    private final SellerRepository sellers;
    private final CaptionParser captionParser;
    private final ReceiverMatcher receiverMatcher;
    private final QrDecoder qrDecoder;
    private final SlipVerificationProvider provider;
    private final SlipCheckProperties properties;
    private final CryptoService crypto;
    private final AuditService audit;
    private final PendingPayloadCache payloadCache;
    private final NotificationRouter notifications;

    // ------------------------------------------------------------------ main entry

    /** Process one submission. Synchronous; adapters decide about threading. */
    public SlipResult process(SlipSubmissionRequest request) {
        Optional<ChannelIdentityEntity> identity =
                identities.findByChannelAndExternalUserId(request.channel().name(), request.externalUserId());

        SlipSubmissionEntity submission = new SlipSubmissionEntity(
                identity.map(ChannelIdentityEntity::getSellerId).orElse(null),
                request.channel().name(),
                request.externalMessageId());
        try {
            // Unique (channel, external_message_id): a webhook redelivery must not process twice.
            submissions.saveAndFlush(submission);
        } catch (DataIntegrityViolationException redelivery) {
            SlipSubmissionEntity existing = submissions
                    .findByChannelAndExternalMessageId(request.channel().name(), request.externalMessageId())
                    .orElseThrow();
            return existing.getResultCode() != null ? toResult(existing) : runPipeline(existing, identity, request);
        }
        return runPipeline(submission, identity, request);
    }

    private SlipResult runPipeline(SlipSubmissionEntity sub, Optional<ChannelIdentityEntity> identity,
                                   SlipSubmissionRequest request) {
        if (identity.isEmpty()) {
            return finalise(sub, ResultCode.NOT_LINKED, null, null, null, null, false);
        }

        // Optional caption: "<amount> <label>". Amount equality is done in satang.
        CaptionResult caption = captionParser.parse(request.caption());
        sub.setCaptionRaw(request.caption());
        sub.setLabel(caption.label());
        if (caption.expectedAmountBaht() != null) {
            sub.setExpectedAmountSatang(toSatang(caption.expectedAmountBaht()));
        }

        // Image validation: size, magic bytes (never trust the extension), decodability.
        byte[] image = request.imageBytes();
        if (image == null || image.length == 0 || image.length > properties.image().maxBytes()
                || !hasImageMagic(image) || !decodesAsImage(image)) {
            return finalise(sub, ResultCode.INVALID_IMAGE, null, captionOrNull(caption), sub.getLabel(), null, false);
        }
        sub.setImageSha256(sha256Hex(image));
        sub.setStatus("PROCESSING");
        submissions.save(sub);

        Optional<String> qr = qrDecoder.decode(image);
        if (qr.isEmpty()) {
            return finalise(sub, ResultCode.UNREADABLE_QR, null, captionOrNull(caption), sub.getLabel(), null, false);
        }

        payloadCache.put(sub.getId(), qr.get());
        return continueVerification(sub, qr.get());
    }

    // ------------------------------------------------------------------ provider onwards

    /**
     * Steps 6–10. Shared by the initial pass and by the retry job, which only has the
     * cached QR payload (the image is gone by then).
     */
    private SlipResult continueVerification(SlipSubmissionEntity sub, String qrPayload) {
        sub.setAttempts(sub.getAttempts() + 1);

        VerifiedSlip slip;
        try {
            slip = provider.verifyByPayload(qrPayload);
        } catch (ProviderException e) {
            return switch (e.error()) {
                case NOT_FOUND, INVALID_PAYLOAD ->
                        finalise(sub, ResultCode.SLIP_NOT_FOUND, null, expectedOf(sub), sub.getLabel(), null, false);
                case QUOTA ->
                        finalise(sub, ResultCode.QUOTA_EXCEEDED, null, expectedOf(sub), sub.getLabel(), null, false);
                case PENDING -> scheduleRetryOrFinalise(sub, ResultCode.BANK_PENDING);
                case UNAVAILABLE -> scheduleRetryOrFinalise(sub, ResultCode.PROVIDER_UNAVAILABLE);
            };
        }

        // Persist only extracted facts — never the image, never the QR payload.
        sub.setProviderName(provider.name());
        sub.setProviderResponse(trim(slip.rawJsonTrimmed()));
        sub.setSendingBank(slip.sendingBank());
        sub.setTransRef(slip.transRef());
        sub.setAmountSatang(toSatang(slip.amountBaht()));
        sub.setTransferredAt(slip.transferredAt());
        sub.setReceiverNameMasked(slip.receiverNameMasked());
        sub.setReceiverAccountMasked(slip.receiverAccountMasked());
        submissions.save(sub);

        // Step 7: receiver check against the seller's encrypted accounts.
        List<ReceiverMatcher.SellerAccount> sellerAccounts =
                accounts.findBySellerIdOrderByCreatedAtAsc(sub.getSellerId()).stream()
                        .map(a -> new ReceiverMatcher.SellerAccount(
                                a.getId(), a.getBankCode(), crypto.decrypt(a.getAccountNoEnc()), a.getHolderName()))
                        .toList();
        ReceiverMatcher.Result match =
                receiverMatcher.match(slip.receiverAccountMasked(), slip.receiverNameMasked(), sellerAccounts);
        if (match.verdict() == ReceiverMatcher.Verdict.MISMATCH) {
            // Never claimed: a wrong-account transfer cannot block the real owner's submission.
            return finalise(sub, ResultCode.WRONG_RECEIVER, slip, expectedOf(sub), sub.getLabel(), null, false);
        }

        // Step 8: claim the transaction. The unique constraint is the duplicate guarantee.
        try {
            claims.saveAndFlush(new ClaimedTransactionEntity(
                    sub.getSellerId(), slip.sendingBank(), slip.transRef(), sub.getId()));
        } catch (DataIntegrityViolationException alreadyClaimed) {
            Instant firstUsedAt = claims
                    .findBySellerIdAndSendingBankAndTransRef(sub.getSellerId(), slip.sendingBank(), slip.transRef())
                    .map(ClaimedTransactionEntity::getClaimedAt)
                    .orElse(null);
            return finalise(sub, ResultCode.DUPLICATE_SLIP, slip, expectedOf(sub), sub.getLabel(), firstUsedAt, false);
        }

        // Step 9: amount check (only when the caption carried an expected amount).
        ResultCode code;
        if (sub.getExpectedAmountSatang() != null
                && !sub.getExpectedAmountSatang().equals(toSatang(slip.amountBaht()))) {
            code = ResultCode.AMOUNT_MISMATCH;   // transaction stays claimed; goes to review
        } else if (match.verdict() == ReceiverMatcher.Verdict.UNCERTAIN) {
            code = ResultCode.RECEIVER_UNCERTAIN; // goes to review
        } else {
            code = ResultCode.VERIFIED;
        }
        return finalise(sub, code, slip, expectedOf(sub), sub.getLabel(), null, false);
    }

    // ------------------------------------------------------------------ retries

    private SlipResult scheduleRetryOrFinalise(SlipSubmissionEntity sub, ResultCode code) {
        List<Integer> backoff = properties.retry().backoffMinutes();
        if (sub.getAttempts() >= properties.retry().maxAttempts() || backoff.isEmpty()) {
            audit.record(sub.getSellerId(), ActorType.SYSTEM, null, "RETRY_EXHAUSTED",
                    "slip_submission", sub.getId().toString(), Map.of("resultCode", code.name()));
            return finalise(sub, code, null, expectedOf(sub), sub.getLabel(), null, false);
        }
        int index = Math.min(Math.max(sub.getAttempts() - 1, 0), backoff.size() - 1);
        sub.setResultCode(code.name());
        sub.setStatus("RETRY_SCHEDULED");
        sub.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(backoff.get(index))));
        submissions.save(sub);
        return new SlipResult(code, sub.getId(), null, expectedOf(sub), sub.getLabel(), null, true);
    }

    /** Called by RetryJob every minute. Re-runs provider-onwards for due submissions. */
    public void processDueRetries() {
        List<SlipSubmissionEntity> due = submissions.findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                "RETRY_SCHEDULED", Instant.now(), PageRequest.of(0, 50));
        for (SlipSubmissionEntity sub : due) {
            try {
                Optional<String> payload = payloadCache.get(sub.getId());
                SlipResult result;
                if (payload.isEmpty()) {
                    // Process restart dropped the in-memory payload (ADR-2): finalise with the last code.
                    ResultCode last = sub.getResultCode() != null
                            ? ResultCode.valueOf(sub.getResultCode()) : ResultCode.PROVIDER_UNAVAILABLE;
                    result = finalise(sub, last, null, expectedOf(sub), sub.getLabel(), null, false);
                } else {
                    result = continueVerification(sub, payload.get());
                }
                if (!result.retryScheduled()) {
                    payloadCache.remove(sub.getId());
                    sendFollowUp(sub, result);
                }
            } catch (Exception e) {
                log.warn("retry processing failed for submission {}", sub.getId(), e);
            }
        }
    }

    /** After a retry reaches a final result, tell the seller on channels where pushes are allowed. */
    private void sendFollowUp(SlipSubmissionEntity sub, SlipResult result) {
        try {
            Channel channel = Channel.valueOf(sub.getChannel());
            identities.findBySellerIdOrderByLinkedAtAsc(sub.getSellerId()).stream()
                    .filter(i -> i.getChannel().equals(channel.name()))
                    .findFirst()
                    .ifPresent(identity -> notifications.notifyResult(
                            channel, identity.getExternalUserId(), identity.getLanguage(), sellerZone(sub), result));
        } catch (Exception e) {
            log.warn("follow-up notification failed for submission {}", sub.getId(), e);
        }
    }

    private ZoneId sellerZone(SlipSubmissionEntity sub) {
        if (sub.getSellerId() == null) {
            return ZoneId.of(properties.summary().defaultTimeZone());
        }
        return sellers.findById(sub.getSellerId())
                .map(s -> {
                    try {
                        return ZoneId.of(s.getTimeZone());
                    } catch (Exception e) {
                        return ZoneId.of(properties.summary().defaultTimeZone());
                    }
                })
                .orElse(ZoneId.of(properties.summary().defaultTimeZone()));
    }

    // ------------------------------------------------------------------ finalisation

    private SlipResult finalise(SlipSubmissionEntity sub, ResultCode code, VerifiedSlip slip,
                                BigDecimal expectedAmountBaht, String label, Instant firstUsedAt,
                                boolean retryScheduled) {
        sub.setResultCode(code.name());
        sub.setStatus("DONE");
        sub.setProcessedAt(Instant.now());
        submissions.save(sub);
        payloadCache.remove(sub.getId());
        audit.record(sub.getSellerId(), ActorType.CHANNEL, sub.getChannel(), "SLIP_PROCESSED",
                "slip_submission", sub.getId().toString(),
                Map.of("resultCode", code.name(), "channel", sub.getChannel()));
        return new SlipResult(code, sub.getId(), slip, expectedAmountBaht, label, firstUsedAt, retryScheduled);
    }

    /** Rebuild a result for a webhook redelivery of an already-processed message. */
    private SlipResult toResult(SlipSubmissionEntity existing) {
        ResultCode code = ResultCode.valueOf(existing.getResultCode());
        VerifiedSlip slip = existing.getTransRef() == null ? null : new VerifiedSlip(
                existing.getSendingBank(), existing.getTransRef(),
                existing.getAmountSatang() == null ? null : baht(existing.getAmountSatang()),
                existing.getTransferredAt(), existing.getReceiverNameMasked(),
                existing.getReceiverAccountMasked(), null);
        Instant firstUsedAt = null;
        if (code == ResultCode.DUPLICATE_SLIP && existing.getSellerId() != null
                && existing.getSendingBank() != null && existing.getTransRef() != null) {
            firstUsedAt = claims.findBySellerIdAndSendingBankAndTransRef(
                            existing.getSellerId(), existing.getSendingBank(), existing.getTransRef())
                    .map(ClaimedTransactionEntity::getClaimedAt).orElse(null);
        }
        return new SlipResult(code, existing.getId(), slip, expectedOf(existing), existing.getLabel(),
                firstUsedAt, "RETRY_SCHEDULED".equals(existing.getStatus()));
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal expectedOf(SlipSubmissionEntity sub) {
        return sub.getExpectedAmountSatang() == null ? null : baht(sub.getExpectedAmountSatang());
    }

    private static BigDecimal captionOrNull(CaptionResult caption) {
        return caption == null ? null : caption.expectedAmountBaht();
    }

    private static BigDecimal baht(long satang) {
        return BigDecimal.valueOf(satang, 2);
    }

    /** Money: BigDecimal in Java, integer satang in the DB. Never floating point. */
    static long toSatang(BigDecimal baht) {
        return baht.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String trim(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.length() <= PROVIDER_RESPONSE_MAX_CHARS ? raw : raw.substring(0, PROVIDER_RESPONSE_MAX_CHARS);
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean decodesAsImage(byte[] data) {
        try {
            return ImageIO.read(new ByteArrayInputStream(data)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasImageMagic(byte[] b) {
        if (b.length < 12) {
            return false;
        }
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return true;
        }
        // GIF: "GIF8"
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return true;
        }
        // WebP: "RIFF" .... "WEBP"
        return b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }
}