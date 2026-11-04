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

/**
 * Links an external chat identity (Telegram chat id / LINE userId) — or the implicit WEB
 * identity — to a seller. Language is stored per identity.
 */
@Entity
@Table(name = "channel_identities")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelIdentityEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column(name = "language", nullable = false)
    private String language = "en";

    @Column(name = "summary_push", nullable = false)
    private boolean summaryPush = false;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    public ChannelIdentityEntity(UUID sellerId, String channel, String externalUserId, String language) {
        this.sellerId = sellerId;
        this.channel = channel;
        this.externalUserId = externalUserId;
        this.language = language == null ? "en" : language;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
    }
}