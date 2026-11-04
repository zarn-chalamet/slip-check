package com.slipcheck.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "language", nullable = false)
    private String language = "en";

    @Column(name = "time_zone", nullable = false)
    private String timeZone = "Asia/Bangkok";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SellerEntity(String email, String passwordHash, String displayName, String language) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.language = language == null ? "en" : language;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}