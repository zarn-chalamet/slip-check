package com.slipcheck.backend.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

import com.slipcheck.backend.core.model.Channel;

/**
 * All web API payload types (nested so request/response shapes stay in one reviewable file).
 * Seller account numbers are never returned — only {@code accountLast4}.
 */
public final class Dtos {

    private Dtos() {
    }

    // ---- auth ----
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String displayName,
            @Pattern(regexp = "en|th|my") String language) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, SellerDto seller) {
    }

    // ---- seller ----
    public record SellerDto(UUID id, String email, String displayName, String language, String timeZone) {
    }

    public record UpdateSellerRequest(
            @Size(max = 120) String displayName,
            @Pattern(regexp = "en|th|my") String language,
            @Size(max = 64) String timeZone) {
    }

    // ---- bank accounts ----
    public record AccountRequest(
            @NotBlank @Size(max = 16) String bankCode,
            @NotBlank @Pattern(regexp = "[0-9][0-9 \\-]{4,24}") String accountNo,
            @NotBlank @Size(max = 160) String holderName) {
    }

    public record AccountDto(UUID id, String bankCode, String accountLast4, String holderName) {
    }

    // ---- chat linking ----
    public record LinkCodeRequest(@NotNull Channel channel) {
    }

    public record LinkCodeResponse(String code, Instant expiresAt) {
    }

    public record ChannelDto(UUID id, Channel channel, String externalUserId, String language,
                             boolean summaryPush, Instant linkedAt) {
    }

    // ---- slips ----
    public record SlipResultDto(
            UUID submissionId, String resultCode, String amountBaht, String transferredAt,
            String sendingBank, String receiverAccountMasked, String expectedAmountBaht,
            String label, String message, boolean retryScheduled) {
    }

    public record SubmissionDto(
            UUID id, String channel, String status, String resultCode, String captionRaw,
            String label, String expectedAmountBaht, String amountBaht, String transferredAt,
            String sendingBank, String transRef, String receiverAccountMasked,
            String reviewDecision, String reviewNote, Instant createdAt) {
    }

    public record PageDto<T>(java.util.List<T> content, int page, int size,
                             long totalElements, int totalPages) {
    }

    public record ReviewRequest(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
            @Size(max = 500) String note) {
    }

    // ---- summary ----
    public record DailySummaryDto(String date, long verified, String totalBaht, long review,
                                  long duplicates, long rejected, String message) {
    }

    // ---- errors ----
    public record ErrorBody(String error, String message) {
    }
}
