package com.slipcheck.backend.adapter.line;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LINE webhook. The raw body is verified against X-Line-Signature BEFORE parsing;
 * invalid signatures get 401. Always returns 200 quickly — processing is async
 * (replies use reply tokens, pushes stay disabled by default).
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class LineWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);

    private final LineSignatureVerifier verifier;
    private final LineBotService botService;
    private final ObjectMapper mapper = new ObjectMapper();

    public LineWebhookController(LineSignatureVerifier verifier, LineBotService botService) {
        this.verifier = verifier;
        this.botService = botService;
    }

    @PostMapping("/line")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            HttpServletRequest request) throws java.io.IOException {
        byte[] rawBody = request.getInputStream().readAllBytes();
        if (!verifier.verify(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            JsonNode root = mapper.readTree(rawBody);
            botService.handleEvents(root);
        } catch (Exception e) {
            log.warn("LINE event handling failed: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
