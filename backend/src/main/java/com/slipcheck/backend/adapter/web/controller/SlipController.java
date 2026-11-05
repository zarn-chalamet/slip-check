package com.slipcheck.backend.adapter.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.slipcheck.backend.adapter.web.dto.Dtos.PageDto;
import com.slipcheck.backend.adapter.web.dto.Dtos.ReviewRequest;
import com.slipcheck.backend.adapter.web.dto.Dtos.SlipResultDto;
import com.slipcheck.backend.adapter.web.dto.Dtos.SubmissionDto;
import com.slipcheck.backend.adapter.web.service.SellerService;
import com.slipcheck.backend.adapter.web.service.SlipSubmissionService;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.i18n.MessageFormatter;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.entity.SlipSubmissionEntity;
import com.slipcheck.backend.security.SecurityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Web adapter: upload, history, review queue. Translates HTTP into service calls —
 * no business rules here (the same SlipService powers the chat bots).
 */
@RestController
@RequestMapping("/api/v1/slips")
@RequiredArgsConstructor
public class SlipController {

    private final SlipSubmissionService slipSubmissions;
    private final SellerService sellerService;
    private final MessageFormatter formatter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SlipResultDto upload(@RequestPart("image") MultipartFile image,
                                @RequestParam(value = "caption", required = false) String caption)
            throws IOException {
        SellerEntity seller = currentSeller();
        ZoneId zone = slipSubmissions.zoneOf(seller);
        SlipResult result = slipSubmissions.upload(seller, image, caption);
        String message = formatter.forResult(result, seller.getLanguage(), zone);
        return toResultDto(result, message, zone);
    }

    @GetMapping
    public PageDto<SubmissionDto> history(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String resultCode,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        SellerEntity seller = currentSeller();
        ZoneId zone = slipSubmissions.zoneOf(seller);
        Page<SlipSubmissionEntity> result = slipSubmissions.history(
                seller.getId(), zone, status, resultCode, from, to, page, size);
        List<SubmissionDto> content = result.getContent().stream()
                .map(s -> toSubmissionDto(s, zone)).toList();
        return new PageDto<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/review")
    public List<SubmissionDto> reviewQueue() {
        SellerEntity seller = currentSeller();
        ZoneId zone = slipSubmissions.zoneOf(seller);
        return slipSubmissions.reviewQueue(seller.getId()).stream()
                .map(s -> toSubmissionDto(s, zone)).toList();
    }

    @GetMapping("/{id}")
    public SubmissionDto one(@PathVariable UUID id) {
        SellerEntity seller = currentSeller();
        return toSubmissionDto(slipSubmissions.get(seller.getId(), id), slipSubmissions.zoneOf(seller));
    }

    @PostMapping("/{id}/review")
    public SubmissionDto review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        SellerEntity seller = currentSeller();
        SlipSubmissionEntity reviewed = slipSubmissions.review(
                seller.getId(), id, request.decision(), request.note());
        return toSubmissionDto(reviewed, slipSubmissions.zoneOf(seller));
    }

    // ------------------------------------------------------------------ mapping

    private SellerEntity currentSeller() {
        return sellerService.getSeller(SecurityUtils.currentSellerId());
    }

    private static SlipResultDto toResultDto(SlipResult result, String message, ZoneId zone) {
        return new SlipResultDto(
                result.submissionId(),
                result.code().name(),
                result.slip() == null ? null : plain(result.slip().amountBaht()),
                result.slip() == null || result.slip().transferredAt() == null ? null
                        : OffsetDateTime.ofInstant(result.slip().transferredAt(), zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                result.slip() == null ? null : result.slip().sendingBank(),
                result.slip() == null ? null : result.slip().receiverAccountMasked(),
                result.expectedAmountBaht() == null ? null : plain(result.expectedAmountBaht()),
                result.label(),
                message,
                result.retryScheduled());
    }

    private static SubmissionDto toSubmissionDto(SlipSubmissionEntity s, ZoneId zone) {
        return new SubmissionDto(
                s.getId(), s.getChannel(), s.getStatus(), s.getResultCode(), s.getCaptionRaw(),
                s.getLabel(), s.getExpectedAmountSatang() == null ? null : plain(s.getExpectedAmountSatang()),
                s.getAmountSatang() == null ? null : plain(s.getAmountSatang()),
                s.getTransferredAt() == null ? null
                        : OffsetDateTime.ofInstant(s.getTransferredAt(), zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                s.getSendingBank(), s.getTransRef(), s.getReceiverAccountMasked(),
                s.getReviewDecision(), s.getReviewNote(), s.getCreatedAt());
    }

    private static String plain(BigDecimal baht) {
        return baht == null ? null : baht.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String plain(long satang) {
        return BigDecimal.valueOf(satang, 2).toPlainString();
    }
}