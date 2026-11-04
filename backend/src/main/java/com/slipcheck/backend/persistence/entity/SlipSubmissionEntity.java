package com.slipcheck.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One slip submission. Holds extracted facts only — never the image itself.
 * (channel, external_message_id) is unique: webhook idempotency.
 */
@Entity
@Table(name = "slip_submissions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlipSubmissionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "external_message_id", nullable = false)
    private String externalMessageId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "caption_raw")
    private String captionRaw;

    @Column(name = "expected_amount_satang")
    private Long expectedAmountSatang;

    @Column(name = "label")
    private String label;

    @Column(name = "sending_bank")
    private String sendingBank;

    @Column(name = "trans_ref")
    private String transRef;

    @Column(name = "amount_satang")
    private Long amountSatang;

    @Column(name = "transferred_at")
    private Instant transferredAt;

    @Column(name = "receiver_name_masked")
    private String receiverNameMasked;

    @Column(name = "receiver_account_masked")
    private String receiverAccountMasked;

    @Column(name = "image_sha256")
    private String imageSha256;

    @Column(name = "provider_name")
    private String providerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private String providerResponse;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "review_decision")
    private String reviewDecision;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public SlipSubmissionEntity(UUID sellerId, String channel, String externalMessageId) {
        this.sellerId = sellerId;
        this.channel = channel;
        this.externalMessageId = externalMessageId;
        this.status = "RECEIVED";
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void review(String decision, String note) {
        this.reviewDecision = decision;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }
}