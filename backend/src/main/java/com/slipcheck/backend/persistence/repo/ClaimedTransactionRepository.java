package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.slipcheck.backend.persistence.entity.ClaimedTransactionEntity;

import java.util.Optional;
import java.util.UUID;

public interface ClaimedTransactionRepository extends JpaRepository<ClaimedTransactionEntity, UUID> {

    Optional<ClaimedTransactionEntity> findBySellerIdAndSendingBankAndTransRef(
            UUID sellerId, String sendingBank, String transRef);

    long countBySellerIdAndSendingBankAndTransRef(UUID sellerId, String sendingBank, String transRef);
}
