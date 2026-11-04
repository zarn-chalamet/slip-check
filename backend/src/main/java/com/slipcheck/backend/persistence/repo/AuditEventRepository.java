package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.slipcheck.backend.persistence.entity.AuditEventEntity;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    /** PDPA deletion: the only permitted UPDATE on the append-only log. */
    @Modifying
    @Query("update AuditEventEntity a set a.sellerId = null, a.actorId = null where a.sellerId = :sellerId")
    int anonymiseForSeller(@Param("sellerId") UUID sellerId);
}
