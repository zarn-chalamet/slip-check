package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.slipcheck.backend.persistence.entity.LinkCodeEntity;

import java.util.Optional;
import java.util.UUID;

public interface LinkCodeRepository extends JpaRepository<LinkCodeEntity, UUID> {

    Optional<LinkCodeEntity> findByCodeHashAndUsedAtIsNull(String codeHash);
}

