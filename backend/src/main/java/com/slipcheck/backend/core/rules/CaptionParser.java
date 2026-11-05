package com.slipcheck.backend.core.rules;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the optional caption that may accompany a slip.
 *
 * If the caption starts with a number, that number is the expected amount in baht and the
 * rest is the label: "590 Aom" -> 590.00 / "Aom"; "1,250.50 order 77" -> 1250.50 / "order 77";
 * "Aom" -> no amount / "Aom".
 *
 * Thai digits (๐-๙) and Burmese digits (၀-၉) are normalised to ASCII first; a trailing
 * ฿ / บาท / THB / baht is ignored.
 */
@Component
public class CaptionParser {

    public record CaptionResult(BigDecimal expectedAmountBaht, String label) {
    }

    private static final Pattern AMOUNT_THEN_LABEL = Pattern.compile(
            "^([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
                    + "\\s*(?:฿|บาท|(?i:thb|baht))?\\s*(.*)$");

    public CaptionResult parse(String caption) {
        if (caption == null || caption.isBlank()) {
            return new CaptionResult(null, null);
        }
        String normalised = normalizeDigits(caption.trim());
        Matcher m = AMOUNT_THEN_LABEL.matcher(normalised);
        if (m.matches()) {
            try {
                BigDecimal amount = new BigDecimal(m.group(1).replace(",", ""));
                String label = m.group(2) == null || m.group(2).isBlank() ? null : m.group(2).trim();
                return new CaptionResult(amount, label);
            } catch (NumberFormatException ignored) {
                // fall through: treat the whole caption as a label
            }
        }
        return new CaptionResult(null, normalised);
    }

    public static String normalizeDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '๐' && c <= '๙') {
                b.append((char) ('0' + (c - '๐')));
            } else if (c >= '၀' && c <= '၉') {
                b.append((char) ('0' + (c - '၀')));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
