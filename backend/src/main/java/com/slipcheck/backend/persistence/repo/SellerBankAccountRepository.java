package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.slipcheck.backend.persistence.entity.SellerBankAccountEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccountEntity, UUID> {

    List<SellerBankAccountEntity> findBySellerIdOrderByCreatedAtAsc(UUID sellerId);

    Optional<SellerBankAccountEntity> findByIdAndSellerId(UUID id, UUID sellerId);
}
