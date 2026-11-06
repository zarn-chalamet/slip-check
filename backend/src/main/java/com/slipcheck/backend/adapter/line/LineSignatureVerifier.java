package com.slipcheck.backend.adapter.line;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * LINE webhook authenticity: HMAC-SHA256 of the RAW request body keyed with the channel
 * secret, Base64-encoded, compared in constant time with the X-Line-Signature header.
 * The secret comes only from LINE_CHANNEL_SECRET and is never logged.
 */
@Component
public class LineSignatureVerifier {

    private final byte[] secret;

    public LineSignatureVerifier(@Value("${line.channel-secret:}") String channelSecret) {
        this.secret = channelSecret == null ? new byte[0] : channelSecret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isConfigured() {
        return secret.length > 0;
    }

    public boolean verify(byte[] rawBody, String signatureHeader) {
        if (!isConfigured() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(rawBody));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
