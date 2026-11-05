package com.slipcheck.backend.adapter.web.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.slipcheck.backend.adapter.web.exception.ImageTooLargeException;
import com.slipcheck.backend.adapter.web.exception.InvalidImageException;
import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.audit.AuditService.ActorType;
import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.core.model.SlipSubmissionRequest;
import com.slipcheck.backend.core.service.SlipService;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.entity.SlipSubmissionEntity;
import com.slipcheck.backend.persistence.repo.SlipSubmissionRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Web adapter service: upload, history, review queue. Translates web calls into core calls —
 * no business rules here (the same SlipService powers the chat bots).
 */
@Service
@RequiredArgsConstructor
public class SlipSubmissionService {

    private final SlipService slipService;
    private final SlipSubmissionRepository submissions;
    private final SlipCheckProperties properties;
    private final AuditService audit;

    public SlipResult upload(SellerEntity seller, MultipartFile image, String caption) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException();
        }
        if (image.getSize() > properties.image().maxBytes()) {
            throw new ImageTooLargeException();
        }
        // The image passes through in memory and is never stored (spec section 15).
        SlipSubmissionRequest request = new SlipSubmissionRequest(
                Channel.WEB,
                seller.getId().toString(),
                "web:" + UUID.randomUUID(),
                image.getBytes(),
                caption,
                seller.getLanguage());
        return slipService.process(request);
    }

    public Page<SlipSubmissionEntity> history(UUID sellerId, ZoneId zone, String status, String resultCode,
                                              String from, String to, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);

        Specification<SlipSubmissionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sellerId"), sellerId));
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (resultCode != null && !resultCode.isBlank()) {
                predicates.add(cb.equal(root.get("resultCode"), resultCode));
            }
            parseBoundary(from, zone).ifPresent(instant ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), instant)));
            parseBoundary(to, zone).ifPresent(instant ->
                    predicates.add(cb.lessThan(root.get("createdAt"), instant)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return submissions.findAll(spec,
                PageRequest.of(Math.max(page, 0), pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<SlipSubmissionEntity> reviewQueue(UUID sellerId) {
        return submissions.findBySellerIdAndResultCodeInAndReviewDecisionIsNullOrderByCreatedAtDesc(
                sellerId, List.of("AMOUNT_MISMATCH", "RECEIVER_UNCERTAIN"));
    }

    public SlipSubmissionEntity get(UUID sellerId, UUID id) {
        return owned(sellerId, id);
    }

    public SlipSubmissionEntity review(UUID sellerId, UUID id, String decision, String note) {
        SlipSubmissionEntity submission = owned(sellerId, id);
        if (submission.getReviewDecision() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already reviewed");
        }
        String code = submission.getResultCode();
        if (!"AMOUNT_MISMATCH".equals(code) && !"RECEIVER_UNCERTAIN".equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission is not in the review queue");
        }
        submission.review(decision, note);
        submissions.save(submission);
        audit.record(submission.getSellerId(), ActorType.SELLER, submission.getSellerId().toString(),
                "APPROVED".equals(decision) ? "REVIEW_APPROVED" : "REVIEW_REJECTED",
                "slip_submission", submission.getId().toString(), Map.of("resultCode", code));
        return submission;
    }

    public ZoneId zoneOf(SellerEntity seller) {
        try {
            return ZoneId.of(seller.getTimeZone());
        } catch (Exception e) {
            return ZoneId.of(properties.summary().defaultTimeZone());
        }
    }

    private SlipSubmissionEntity owned(UUID sellerId, UUID id) {
        return submissions.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private static java.util.Optional<java.time.Instant> parseBoundary(String value, ZoneId zone) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(java.time.Instant.parse(value.trim()));
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return java.util.Optional.of(LocalDate.parse(value.trim()).atStartOfDay(zone).toInstant());
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }
}