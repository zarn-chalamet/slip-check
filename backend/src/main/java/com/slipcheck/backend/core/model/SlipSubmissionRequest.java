package com.slipcheck.backend.core.model;

/**
 * Channel-agnostic input to the core pipeline. Adapters convert their update/event/upload
 * shapes into this record. The image bytes are processed in memory and never stored.
 */
public record SlipSubmissionRequest(
        Channel channel,
        String externalUserId,
        String externalMessageId,
        byte[] imageBytes,
        String caption,
        String language) {
}
