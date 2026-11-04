package com.slipcheck.backend.audit;

import org.springframework.stereotype.Service;

import com.slipcheck.backend.persistence.entity.AuditEventEntity;
import com.slipcheck.backend.persistence.repo.AuditEventRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit trail. Never log images, QR payloads, full account numbers,
 * tokens or API keys in payloads.
 *
 * Event types used: SELLER_REGISTERED, SLIP_PROCESSED, REVIEW_APPROVED, REVIEW_REJECTED,
 * ACCOUNT_ADDED, ACCOUNT_REMOVED, CHAT_LINKED, CHAT_UNLINKED, LANGUAGE_CHANGED,
 * SELLER_DELETED, RETRY_EXHAUSTED.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(UUID sellerId, ActorType actorType, String actorId, String eventType,
                       String entityType, String entityId, Map<String, Object> payload) {
        repository.save(new AuditEventEntity(
                sellerId, actorType.name(), actorId, eventType, entityType, entityId, payload));
    }

    public enum ActorType {
        SELLER, SYSTEM, CHANNEL
    }
}
