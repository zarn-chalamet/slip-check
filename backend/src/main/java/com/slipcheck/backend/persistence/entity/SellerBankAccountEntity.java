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
 * A seller's receiving bank account. The full account number is stored AES-GCM encrypted;
 * only the last 4 digits are ever displayed.
 */
@Entity
@Table(name = "seller_bank_accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerBankAccountEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "account_no_enc", nullable = false)
    private byte[] accountNoEnc;

    @Column(name = "account_last4", nullable = false)
    private String accountLast4;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SellerBankAccountEntity(UUID sellerId, String bankCode, byte[] accountNoEnc,
                                   String accountLast4, String holderName) {
        this.sellerId = sellerId;
        this.bankCode = bankCode;
        this.accountNoEnc = accountNoEnc;
        this.accountLast4 = accountLast4;
        this.holderName = holderName;
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
}