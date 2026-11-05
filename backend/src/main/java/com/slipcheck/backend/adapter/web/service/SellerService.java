package com.slipcheck.backend.adapter.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slipcheck.backend.adapter.web.exception.CannotUnlinkWebChannelException;
import com.slipcheck.backend.adapter.web.exception.ChannelNotFoundException;
import com.slipcheck.backend.adapter.web.exception.InvalidTimeZoneException;
import com.slipcheck.backend.adapter.web.exception.SellerNotFoundException;
import com.slipcheck.backend.adapter.web.exception.UnsupportedChannelException;
import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.audit.AuditService.ActorType;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.repo.AuditEventRepository;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.security.LinkCodeService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellers;
    private final ChannelIdentityRepository identities;
    private final AuditEventRepository auditEvents;
    private final LinkCodeService linkCodes;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public SellerEntity getSeller(UUID sellerId) {
        return sellers.findById(sellerId).orElseThrow(SellerNotFoundException::new);
    }

    @Transactional
    public SellerEntity updateProfile(UUID sellerId, String displayName, String timeZone, String language) {
        SellerEntity seller = getSeller(sellerId);

        if (displayName != null && !displayName.isBlank()) {
            seller.setDisplayName(displayName.trim());
        }
        if (timeZone != null && !timeZone.isBlank()) {
            try {
                ZoneId.of(timeZone);
            } catch (DateTimeException e) {
                throw new InvalidTimeZoneException();
            }
            seller.setTimeZone(timeZone);
        }
        if (language != null && !language.equals(seller.getLanguage())) {
            seller.setLanguage(language);
            audit.record(seller.getId(), ActorType.SELLER, seller.getId().toString(), "LANGUAGE_CHANGED",
                    "seller", seller.getId().toString(), Map.of("language", language));
        }
        return sellers.save(seller);
    }

    /** PDPA deletion: removes the seller (cascading their data) and anonymises their audit rows. */
    @Transactional
    public void deleteSeller(UUID sellerId) {
        SellerEntity seller = getSeller(sellerId);
        auditEvents.anonymiseForSeller(seller.getId());
        sellers.delete(seller);
    }

    // ---- chat linking ----

    public LinkCodeService.CreatedCode createLinkCode(UUID sellerId, Channel channel) {
        if (channel == Channel.WEB) {
            throw new UnsupportedChannelException();
        }
        return linkCodes.create(sellerId);
    }

    @Transactional(readOnly = true)
    public List<ChannelIdentityEntity> listChannels(UUID sellerId) {
        return identities.findBySellerIdOrderByLinkedAtAsc(sellerId);
    }

    @Transactional
    public void unlinkChannel(UUID sellerId, UUID identityId) {
        ChannelIdentityEntity identity = identities.findByIdAndSellerId(identityId, sellerId)
                .orElseThrow(ChannelNotFoundException::new);
        if (Channel.WEB.name().equals(identity.getChannel())) {
            throw new CannotUnlinkWebChannelException();
        }
        identities.delete(identity);
        audit.record(sellerId, ActorType.SELLER, sellerId.toString(), "CHAT_UNLINKED",
                "channel_identity", identityId.toString(), Map.of("channel", identity.getChannel()));
    }
}