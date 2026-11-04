package com.slipcheck.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.slipcheck.backend.core.model.Channel;

import java.util.List;

/**
 * Typed access to the "slipcheck.*" knobs from application.yml.
 */
@ConfigurationProperties(prefix = "slipcheck")
public record SlipCheckProperties(
        Image image,
        Retry retry,
        Receiver receiver,
        Retention retention,
        Summary summary,
        RateLimit rateLimit,
        String frontendOrigin) {

    public record Image(long maxBytes) {
    }

    public record Retry(List<Integer> backoffMinutes, int maxAttempts, List<Channel> followupChannels) {
    }

    public record Receiver(int minVisibleDigits) {
    }

    public record Retention(int providerResponseDays) {
    }

    public record Summary(String defaultTimeZone, int sendHourLocal) {
    }

    public record RateLimit(int uploadPerMinutePerSeller, int loginPerMinutePerIp) {
    }
}
