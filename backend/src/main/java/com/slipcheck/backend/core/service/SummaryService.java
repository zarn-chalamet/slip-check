package com.slipcheck.backend.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.persistence.repo.SlipSubmissionRepository;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Daily summary per seller (spec F5): verified count + total, items needing review,
 * duplicates and rejections for a local day in the seller's time zone.
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Plain data — formatting into text happens in MessageFormatter / the web DTO. */
    public record DailySummary(String date, long verified, BigDecimal totalBaht,
                               long review, long duplicates, long rejected) {
    }

    private final SlipSubmissionRepository submissions;
    private final SellerRepository sellers;
    private final SlipCheckProperties properties;

    /** For callers that only have an id (e.g. scheduled pushes). */
    @Transactional(readOnly = true)
    public DailySummary compile(UUID sellerId, LocalDate date) {
        ZoneId zone = sellerZone(sellerId);
        return compile(sellerId, zone, date);
    }

    /** For callers that already loaded the seller — avoids a second lookup. */
    @Transactional(readOnly = true)
    public DailySummary compile(SellerEntity seller, LocalDate date) {
        return compile(seller.getId(), zoneOf(seller.getTimeZone()), date);
    }

    public ZoneId sellerZone(UUID sellerId) {
        return sellers.findById(sellerId)
                .map(seller -> zoneOf(seller.getTimeZone()))
                .orElseGet(this::defaultZone);
    }

    private DailySummary compile(UUID sellerId, ZoneId zone, LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(zone);
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();

        long verified = submissions.countByResultBetween(sellerId, "VERIFIED", from, to);
        long totalSatang = submissions.sumVerifiedSatangBetween(sellerId, from, to);
        long review = submissions.countByResultsBetween(
                sellerId, List.of("AMOUNT_MISMATCH", "RECEIVER_UNCERTAIN"), from, to);
        long duplicates = submissions.countByResultBetween(sellerId, "DUPLICATE_SLIP", from, to);
        long rejected = submissions.countByReviewDecisionBetween(sellerId, "REJECTED", from, to)
                + submissions.countByResultsBetween(
                sellerId, List.of("WRONG_RECEIVER", "SLIP_NOT_FOUND"), from, to);

        return new DailySummary(day.format(DAY), verified, BigDecimal.valueOf(totalSatang, 2),
                review, duplicates, rejected);
    }

    private ZoneId zoneOf(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException | NullPointerException e) {
            return defaultZone();
        }
    }

    private ZoneId defaultZone() {
        return ZoneId.of(properties.summary().defaultTimeZone());
    }
}