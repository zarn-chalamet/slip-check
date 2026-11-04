package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.slipcheck.backend.persistence.entity.SlipSubmissionEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlipSubmissionRepository
        extends JpaRepository<SlipSubmissionEntity, UUID>, JpaSpecificationExecutor<SlipSubmissionEntity> {

    Optional<SlipSubmissionEntity> findByChannelAndExternalMessageId(String channel, String externalMessageId);

    Optional<SlipSubmissionEntity> findByIdAndSellerId(UUID id, UUID sellerId);

    List<SlipSubmissionEntity> findBySellerIdAndResultCodeInAndReviewDecisionIsNullOrderByCreatedAtDesc(
            UUID sellerId, Collection<String> resultCodes);

    List<SlipSubmissionEntity> findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            String status, Instant now, org.springframework.data.domain.Pageable pageable);

    @Query("select count(s) from SlipSubmissionEntity s "
            + "where s.sellerId = :sellerId and s.resultCode = :code "
            + "and s.createdAt >= :from and s.createdAt < :to")
    long countByResultBetween(@Param("sellerId") UUID sellerId, @Param("code") String code,
                              @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(s) from SlipSubmissionEntity s "
            + "where s.sellerId = :sellerId and s.resultCode in :codes "
            + "and s.createdAt >= :from and s.createdAt < :to")
    long countByResultsBetween(@Param("sellerId") UUID sellerId, @Param("codes") Collection<String> codes,
                               @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(s.amountSatang), 0) from SlipSubmissionEntity s "
            + "where s.sellerId = :sellerId and s.resultCode = 'VERIFIED' "
            + "and s.createdAt >= :from and s.createdAt < :to")
    long sumVerifiedSatangBetween(@Param("sellerId") UUID sellerId,
                                  @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(s) from SlipSubmissionEntity s "
            + "where s.sellerId = :sellerId and s.reviewDecision = :decision "
            + "and s.createdAt >= :from and s.createdAt < :to")
    long countByReviewDecisionBetween(@Param("sellerId") UUID sellerId, @Param("decision") String decision,
                                      @Param("from") Instant from, @Param("to") Instant to);

    /** Retention job: wipe trimmed provider payloads older than the cutoff. */
    @Modifying
    @Query("update SlipSubmissionEntity s set s.providerResponse = null "
            + "where s.providerResponse is not null and s.createdAt < :cutoff")
    int clearProviderResponsesOlderThan(@Param("cutoff") Instant cutoff);
}

