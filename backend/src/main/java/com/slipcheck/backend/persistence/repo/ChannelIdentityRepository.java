package com.slipcheck.backend.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelIdentityRepository extends JpaRepository<ChannelIdentityEntity, UUID> {

    Optional<ChannelIdentityEntity> findByChannelAndExternalUserId(String channel, String externalUserId);

    List<ChannelIdentityEntity> findBySellerIdOrderByLinkedAtAsc(UUID sellerId);

    List<ChannelIdentityEntity> findBySummaryPushTrue();

    Optional<ChannelIdentityEntity> findByIdAndSellerId(UUID id, UUID sellerId);
}
