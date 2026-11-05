package com.slipcheck.backend.adapter.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slipcheck.backend.adapter.web.dto.Dtos.ChannelDto;
import com.slipcheck.backend.adapter.web.dto.Dtos.LinkCodeRequest;
import com.slipcheck.backend.adapter.web.dto.Dtos.LinkCodeResponse;
import com.slipcheck.backend.adapter.web.dto.Dtos.SellerDto;
import com.slipcheck.backend.adapter.web.dto.Dtos.UpdateSellerRequest;
import com.slipcheck.backend.adapter.web.service.SellerService;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.security.LinkCodeService;
import com.slipcheck.backend.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/me")
    public SellerDto me() {
        return AuthController.toDto(sellerService.getSeller(SecurityUtils.currentSellerId()));
    }

    @PatchMapping("/me")
    public SellerDto updateMe(@Valid @RequestBody UpdateSellerRequest request) {
        return AuthController.toDto(sellerService.updateProfile(
                SecurityUtils.currentSellerId(),
                request.displayName(), request.timeZone(), request.language()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        sellerService.deleteSeller(SecurityUtils.currentSellerId());
        return ResponseEntity.noContent().build();
    }

    // ---- chat linking ----

    @PostMapping("/link-codes")
    public ResponseEntity<LinkCodeResponse> createLinkCode(@Valid @RequestBody LinkCodeRequest request) {
        LinkCodeService.CreatedCode created =
                sellerService.createLinkCode(SecurityUtils.currentSellerId(), request.channel());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LinkCodeResponse(created.code(), created.expiresAt()));
    }

    @GetMapping("/channels")
    public List<ChannelDto> channels() {
        return sellerService.listChannels(SecurityUtils.currentSellerId()).stream()
                .map(SellerController::toChannelDto)
                .toList();
    }

    @DeleteMapping("/channels/{id}")
    public ResponseEntity<Void> unlinkChannel(@PathVariable UUID id) {
        sellerService.unlinkChannel(SecurityUtils.currentSellerId(), id);
        return ResponseEntity.noContent().build();
    }

    static ChannelDto toChannelDto(ChannelIdentityEntity identity) {
        return new ChannelDto(identity.getId(), Channel.valueOf(identity.getChannel()),
                maskedExternalId(identity), identity.getLanguage(), identity.isSummaryPush(),
                identity.getLinkedAt());
    }

    /** Chat ids are quasi-personal; don't leak the full id in list responses. */
    private static String maskedExternalId(ChannelIdentityEntity identity) {
        String external = identity.getExternalUserId();
        if (external == null || external.length() <= 4 || Channel.WEB.name().equals(identity.getChannel())) {
            return Channel.WEB.name().equals(identity.getChannel()) ? "web" : external;
        }
        return "…" + external.substring(external.length() - 4);
    }
}