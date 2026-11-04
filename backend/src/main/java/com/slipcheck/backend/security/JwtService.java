package com.slipcheck.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Issues and validates short-lived HS256 tokens for the seller dashboard. */
@Service
public class JwtService {

    private static final Duration TTL = Duration.ofHours(12);

    private final SecretKey key;

    public JwtService(@Value("${slipcheck.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("slipcheck.jwt-secret (JWT_SECRET) must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issue(UUID sellerId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(sellerId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + TTL.toMillis()))
                .signWith(key)
                .compact();
    }

    public Optional<UUID> parse(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
