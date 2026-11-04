package com.slipcheck.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.slipcheck.backend.config.SlipCheckProperties;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory sliding-window rate limiting (spec section 15):
 *  - POST /api/v1/auth/login → per IP (default 10/min)
 *  - POST /api/v1/slips      → per seller id (or IP if anonymous) (default 30/min)
 * Registered inside the security chain after the JWT filter; not a servlet-container bean.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;

    private final SlipCheckProperties properties;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(SlipCheckProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = null;
        int limit = -1;

        if ("POST".equals(request.getMethod())) {
            String path = request.getRequestURI();
            if (path.equals("/api/v1/auth/login")) {
                limit = properties.rateLimit().loginPerMinutePerIp();
                key = "login:" + clientIp(request);
            } else if (path.equals("/api/v1/slips")) {
                limit = properties.rateLimit().uploadPerMinutePerSeller();
                String principal = SecurityContextHolderAccess.principalOrNull();
                key = "upload:" + (principal != null ? principal : clientIp(request));
            }
        }

        if (limit > 0 && exceeded(key, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests. Slow down and try again.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean exceeded(String key, int limit) {
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
        if (deque.size() >= limit) {
            return true;
        }
        deque.addLast(Instant.now());
        if (hits.size() > 50_000) {
            hits.clear(); // crude bound for memory; windows are 60 s
        }
        return false;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Indirection to keep the security-context access testable and null-safe. */
    static final class SecurityContextHolderAccess {
        private SecurityContextHolderAccess() {
        }

        static String principalOrNull() {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            return auth == null ? null : String.valueOf(auth.getPrincipal());
        }
    }
}
