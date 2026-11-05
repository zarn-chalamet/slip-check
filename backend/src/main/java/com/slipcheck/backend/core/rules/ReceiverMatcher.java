package com.slipcheck.backend.core.rules;

import org.springframework.stereotype.Component;

import com.slipcheck.backend.config.SlipCheckProperties;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Matches a slip's masked receiver account / name against the seller's registered accounts.
 *
 * Slips mask part of the account number (x, X, *, ...) and shorten surnames, so matching is
 * position-wise over visible digits with wildcards, plus:
 *  - a suffix/substring rule for different-length formats (e.g. proxy identifiers),
 *  - a minimum count of visible digits (default 3) below which the verdict is UNCERTAIN,
 *  - a name signal that can upgrade UNCERTAIN to MATCH only if at least one digit matched.
 *
 * Verdicts: MATCH, MISMATCH (definite conflict at a visible position), UNCERTAIN.
 */
@Component
public class ReceiverMatcher {

    public enum Verdict {MATCH, MISMATCH, UNCERTAIN}

    public record SellerAccount(UUID id, String bankCode, String accountDigits, String holderName) {
    }

    public record Result(Verdict verdict, UUID matchedAccountId) {
    }

    private static final Set<Character> WILDCARDS = Set.of('x', 'X', '*', '?', '•', '_', '-', ' ', '‑');
    /** Mask characters that hide digits. Dashes/spaces are stripped before matching instead. */

    private static final Set<Character> MASK_CHARS = Set.of('x', 'X', '*', '•');

    private static final Set<String> TITLES = Set.of(
            "นาย", "นาง", "นางสาว", "นส.", "น.ส.", "ด.ช.", "ด.ญ.",
            "mr.", "mr", "mrs.", "mrs", "ms.", "ms", "miss");

    private final int minVisibleDigits;

    public ReceiverMatcher(SlipCheckProperties properties) {
        this.minVisibleDigits = Math.max(1, properties.receiver().minVisibleDigits());
    }

    public Result match(String receiverAccountMasked, String receiverNameMasked, List<SellerAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return new Result(Verdict.UNCERTAIN, null);
        }
        String masked = normalizeMasked(receiverAccountMasked);

        boolean anyConflict = false;
        SellerAccount partialByDigits = null;

        for (SellerAccount account : accounts) {
            String digits = account.accountDigits() == null ? "" : account.accountDigits().replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                continue;
            }
            Comparison comparison = compare(masked, digits);
            switch (comparison.status()) {
                case MATCH -> {
                    return new Result(Verdict.MATCH, account.id());
                }
                case CONFLICT -> anyConflict = true;
                case PARTIAL -> {
                    if (partialByDigits == null) {
                        partialByDigits = account;
                    }
                }
                case NO_SIGNAL -> {
                    // unknown format: no contribution either way
                }
            }
        }

        // Name signal: can upgrade a digit-partial to MATCH, never decides on its own.
        if (partialByDigits != null && nameMatches(receiverNameMasked, partialByDigits.holderName())) {
            return new Result(Verdict.MATCH, partialByDigits.id());
        }
        if (partialByDigits != null) {
            return new Result(Verdict.UNCERTAIN, null);
        }
        return anyConflict ? new Result(Verdict.MISMATCH, null) : new Result(Verdict.UNCERTAIN, null);
    }

    // ------------------------------------------------------------------ digits

    private enum Status {MATCH, CONFLICT, PARTIAL, NO_SIGNAL}

    private record Comparison(Status status) {
    }

    private static final Comparison CONFLICT = new Comparison(Status.CONFLICT);
    private static final Comparison MATCHED = new Comparison(Status.MATCH);
    private static final Comparison PARTIAL = new Comparison(Status.PARTIAL);
    private static final Comparison NO_SIGNAL = new Comparison(Status.NO_SIGNAL);

    /**
     * Normalise the slip's masked account: strip spaces/dashes, uppercase.
     * Mask characters stay; anything else that is not a digit makes the format unknown.
     */
    static String normalizeMasked(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(raw.length());
        for (char c : raw.toCharArray()) {
            if (c == ' ' || c == '-' || c == '‑') {
                continue;
            }
            b.append(Character.toUpperCase(c));
        }
        return b.toString();
    }

    private Comparison compare(String masked, String digits) {
        if (masked.isEmpty()) {
            return NO_SIGNAL;
        }
        int visible = 0;
        for (char c : masked.toCharArray()) {
            if (isMaskChar(c)) {
                continue;
            }
            if (!Character.isDigit(c)) {
                return NO_SIGNAL; // unknown format (e.g. promptpay proxy id) -> uncertain
            }
            visible++;
        }
        if (visible == 0) {
            return NO_SIGNAL;
        }

        if (masked.length() == digits.length()) {
            boolean conflict = false;
            for (int i = 0; i < masked.length(); i++) {
                char c = masked.charAt(i);
                if (isMaskChar(c)) {
                    continue;
                }
                if (c != digits.charAt(i)) {
                    conflict = true;
                    break;
                }
            }
            if (conflict) {
                return CONFLICT;
            }
            return visible >= minVisibleDigits ? MATCHED : PARTIAL;
        }

        // Different length / format: compare the longest run of visible digits as a
        // suffix/substring of the seller's digits. No containment here is NOT a definite
        // mismatch (proxy identifiers legitimately differ) -> NO_SIGNAL (-> UNCERTAIN).
        String run = longestVisibleDigitRun(masked);
        if (run.length() >= minVisibleDigits) {
            return (digits.endsWith(run) || digits.contains(run)) ? MATCHED : NO_SIGNAL;
        }
        return run.isEmpty() ? NO_SIGNAL : PARTIAL_FOR(digits, run);
    }

    private static Comparison PARTIAL_FOR(String digits, String run) {
        return digits.contains(run) ? PARTIAL : NO_SIGNAL;
    }

    static boolean isMaskChar(char c) {
        return MASK_CHARS.contains(c) || WILDCARDS.contains(c);
    }

    static String longestVisibleDigitRun(String masked) {
        String best = "";
        StringBuilder current = new StringBuilder();
        for (char c : masked.toCharArray()) {
            if (Character.isDigit(c)) {
                current.append(c);
            } else {
                if (current.length() > best.length()) {
                    best = current.toString();
                }
                current.setLength(0);
            }
        }
        if (current.length() > best.length()) {
            best = current.toString();
        }
        return best;
    }

    // ------------------------------------------------------------------ name

    /**
     * Masked receiver name vs. seller holder name: case-insensitive, titles ignored,
     * truncated surname allowed (masked surname may be a prefix of the holder's surname,
     * e.g. "Somsak J." vs "Somsak Jaidee").
     */
    static boolean nameMatches(String maskedName, String holderName) {
        if (maskedName == null || holderName == null) {
            return false;
        }
        String[] masked = cleanName(maskedName);
        String[] holder = cleanName(holderName);
        if (masked.length == 0 || holder.length == 0) {
            return false;
        }
        // First token (given name) must match exactly.
        if (!masked[0].equals(holder[0])) {
            return false;
        }
        if (masked.length == 1 || holder.length == 1) {
            return true; // given name matched, nothing more to compare
        }
        String maskedSurname = masked[masked.length - 1];
        String holderSurname = holder[holder.length - 1];
        return holderSurname.equals(maskedSurname)
                || holderSurname.startsWith(maskedSurname)
                || maskedSurname.startsWith(holderSurname);
    }

    private static String[] cleanName(String name) {
        String cleaned = name.toLowerCase(Locale.ROOT).replaceAll("[.,]", " ").trim();
        String[] tokens = cleaned.split("\\s+");
        java.util.List<String> out = new java.util.ArrayList<>(tokens.length);
        for (String t : tokens) {
            if (!t.isBlank() && !TITLES.contains(t)) {
                out.add(t);
            }
        }
        return out.toArray(new String[0]);
    }
}
