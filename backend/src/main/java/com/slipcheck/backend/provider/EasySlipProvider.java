package com.slipcheck.backend.provider;

import com.slipcheck.backend.core.model.ProviderException;
import com.slipcheck.backend.core.model.ProviderException.ProviderError;
import com.slipcheck.backend.core.model.VerifiedSlip;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Real provider integration for EasySlip API v2 (https://easyslip.com).
 * Active when provider.name=easyslip in application.yml.
 */
@Component
@ConditionalOnProperty(name = "provider.name", havingValue = "easyslip")
public class EasySlipProvider implements SlipVerificationProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public EasySlipProvider(
            @Value("${provider.base-url:https://api.easyslip.com/v2/verify/bank}") String baseUrl,
            @Value("${provider.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String name() {
        return "easyslip";
    }

    @Override
    public VerifiedSlip verifyByPayload(String qrPayload) throws ProviderException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ProviderException(ProviderError.UNAVAILABLE, "EasySlip API Key is missing in application.yml");
        }
        if (qrPayload == null || qrPayload.isBlank()) {
            throw new ProviderException(ProviderError.INVALID_PAYLOAD, "QR payload is empty");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("payload", qrPayload.trim()));

            String responseBody = restClient.post()
                    .body(requestBody)
                    .exchange((req, res) -> new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8));

            return parseResponse(responseBody);

        } catch (ProviderException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ProviderException(ProviderError.UNAVAILABLE, "Failed to connect to EasySlip: " + e.getMessage());
        }
    }

    private VerifiedSlip parseResponse(String json) throws ProviderException {
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean success = root.path("success").asBoolean(false);

            if (!success) {
                String errorCode = root.path("error").path("code").asString("");
                String message = root.path("error").path("message").asString("Unknown error");

                ProviderError errorType = switch (errorCode) {
                    case "QUOTA_EXCEEDED" -> ProviderError.QUOTA;
                    case "SLIP_NOT_FOUND" -> ProviderError.NOT_FOUND;
                    case "VALIDATION_ERROR" -> ProviderError.INVALID_PAYLOAD;
                    default -> ProviderError.UNAVAILABLE;
                };
                throw new ProviderException(errorType, message);
            }

            JsonNode rawSlip = root.path("data").path("rawSlip");

            String transRef = rawSlip.path("transRef").asString();
            String sendingBank = rawSlip.path("sender").path("bank").path("short").asString("UNKNOWN");
            BigDecimal amount = new BigDecimal(rawSlip.path("amount").path("amount").asString("0"));

            Instant transferredAt = Instant.now();
            try {
                transferredAt = OffsetDateTime.parse(rawSlip.path("date").asString()).toInstant();
            } catch (Exception ignored) {}

            String senderName = rawSlip.path("sender").path("account").path("name").path("th").asString();
            if (senderName.isBlank()) {
                senderName = rawSlip.path("sender").path("account").path("name").path("en").asString("Unknown");
            }

            JsonNode receiverAcc = rawSlip.path("receiver").path("account");
            String receiverMasked = receiverAcc.path("bank").path("account").asString("");
            if (receiverMasked.isBlank()) {
                receiverMasked = receiverAcc.path("proxy").path("account").asString("");
            }

            return new VerifiedSlip(
                    sendingBank,
                    transRef,
                    amount,
                    transferredAt,
                    senderName,
                    receiverMasked,
                    json
            );

        } catch (ProviderException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ProviderException(ProviderError.INVALID_PAYLOAD, "Cannot parse EasySlip response: " + e.getMessage());
        }
    }
}