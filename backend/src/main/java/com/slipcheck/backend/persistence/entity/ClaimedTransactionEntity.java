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

import java.time.Instant;
import java.util.UUID;

/**
 * The duplicate guarantee: a real transaction sent to a seller's account may be claimed
 * once. The database unique constraint on (seller_id, sending_bank, trans_ref) — not an
 * application-level check — is what enforces this under concurrency.
 */
@Entity
@Table(name = "claimed_transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimedTransactionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "sending_bank", nullable = false)
    private String sendingBank;

    @Column(name = "trans_ref", nullable = false)
    private String transRef;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    public ClaimedTransactionEntity(UUID sellerId, String sendingBank, String transRef, UUID submissionId) {
        this.sellerId = sellerId;
        this.sendingBank = sendingBank;
        this.transRef = transRef;
        this.submissionId = submissionId;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (claimedAt == null) {
            claimedAt = Instant.now();
        }
    }
}