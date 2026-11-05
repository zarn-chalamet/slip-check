package com.slipcheck.backend.adapter.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slipcheck.backend.adapter.web.dto.Dtos.AuthResult;
import com.slipcheck.backend.adapter.web.exception.EmailTakenException;
import com.slipcheck.backend.adapter.web.exception.InvalidCredentialsException;
import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.entity.SellerEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;
import com.slipcheck.backend.persistence.repo.SellerRepository;
import com.slipcheck.backend.security.JwtService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SellerRepository sellers;
    private final ChannelIdentityRepository identities;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    @Transactional
    public AuthResult register(String email, String rawPassword, String displayName, String language) {
        String normalizedEmail = email.trim();
        if (sellers.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailTakenException();
        }
        String lang = language == null ? "en" : language;

        SellerEntity seller = new SellerEntity(normalizedEmail, passwordEncoder.encode(rawPassword),
                displayName.trim(), lang);
        sellers.save(seller);

        // The implicit WEB identity lets the core resolve web uploads like any channel (ADR-5).
        identities.save(new ChannelIdentityEntity(seller.getId(), Channel.WEB.name(),
                seller.getId().toString(), lang));

        audit.record(seller.getId(), AuditService.ActorType.SELLER, seller.getId().toString(), "SELLER_REGISTERED",
                "seller", seller.getId().toString(), Map.of("language", lang));

        return new AuthResult(jwtService.issue(seller.getId()), seller);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        SellerEntity seller = sellers.findByEmailIgnoreCase(email.trim())
                .filter(s -> passwordEncoder.matches(rawPassword, s.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new AuthResult(jwtService.issue(seller.getId()), seller);
    }
}