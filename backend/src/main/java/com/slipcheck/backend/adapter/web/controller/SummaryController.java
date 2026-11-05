package com.slipcheck.backend.adapter.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slipcheck.backend.adapter.web.dto.Dtos.DailySummaryDto;
import com.slipcheck.backend.adapter.web.exception.InvalidDateException;
import com.slipcheck.backend.adapter.web.service.SellerService;
import com.slipcheck.backend.core.service.SummaryService;
import com.slipcheck.backend.core.service.SummaryService.DailySummary;
import com.slipcheck.backend.i18n.MessageFormatter;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.security.SecurityUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final SellerService sellerService;
    private final MessageFormatter formatter;

    @GetMapping("/daily")
    public DailySummaryDto daily(@RequestParam(required = false) String date) {
        SellerEntity seller = sellerService.getSeller(SecurityUtils.currentSellerId());
        DailySummary summary = summaryService.compile(seller, parseDate(date));
        return toDto(summary, seller.getLanguage());
    }

    private DailySummaryDto toDto(DailySummary summary, String language) {
        String message = formatter.dailySummary(summary.date(), summary.verified(), summary.totalBaht(),
                summary.review(), summary.duplicates(), summary.rejected(), language);
        return new DailySummaryDto(summary.date(), summary.verified(),
                MessageFormatter.amount(summary.totalBaht()), summary.review(),
                summary.duplicates(), summary.rejected(), message);
    }

    /** Blank or missing means "today in the seller's zone" (the service decides). */
    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidDateException();
        }
    }
}