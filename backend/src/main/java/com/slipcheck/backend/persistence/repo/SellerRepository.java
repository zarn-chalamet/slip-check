package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.slipcheck.backend.persistence.entity.SellerEntity;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<SellerEntity, UUID> {

    Optional<SellerEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
