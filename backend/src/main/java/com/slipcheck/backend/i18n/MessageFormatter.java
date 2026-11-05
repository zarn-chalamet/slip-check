package com.slipcheck.backend.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.core.model.ResultCode;
import com.slipcheck.backend.core.model.SlipResult;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns a {@link SlipResult} code + data into text using the user's language.
 * The core never contains user-facing strings — only keys (see MessageKeys).
 * Falls back to English when a key is missing in the requested bundle.
 */
@Component
public class MessageFormatter {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);
    private static final DecimalFormat AMOUNT = new DecimalFormat("#,##0.00");
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Bangkok");

    private final MessageSource messageSource;

    public MessageFormatter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String format(String key, String language, Object... args) {
        Locale locale = locale(language);
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return messageSource.getMessage(key, args, Locale.ENGLISH);
        }
    }

    /** Format the reply text for a slip result. */
    public String forResult(SlipResult result, String language, ZoneId sellerZone) {
        ZoneId zone = sellerZone != null ? sellerZone : FALLBACK_ZONE;
        ResultCode code = result.code();
        return switch (code) {
            case VERIFIED -> format(MessageKeys.SLIP_VERIFIED, language,
                    amount(result.slip().amountBaht()),
                    dateTime(result.slip().transferredAt(), zone),
                    result.slip().sendingBank());
            case AMOUNT_MISMATCH -> format(MessageKeys.SLIP_AMOUNT_MISMATCH, language,
                    amount(result.slip().amountBaht()),
                    dateTime(result.slip().transferredAt(), zone),
                    result.slip().sendingBank(),
                    amount(result.expectedAmountBaht()));
            case RECEIVER_UNCERTAIN -> format(MessageKeys.SLIP_RECEIVER_UNCERTAIN, language,
                    amount(result.slip().amountBaht()),
                    dateTime(result.slip().transferredAt(), zone));
            case DUPLICATE_SLIP -> format(MessageKeys.SLIP_DUPLICATE, language,
                    "", dateTime(result.firstUsedAt(), zone));
            case WRONG_RECEIVER -> format(MessageKeys.SLIP_WRONG_RECEIVER, language);
            case UNREADABLE_QR -> format(MessageKeys.SLIP_UNREADABLE_QR, language);
            case SLIP_NOT_FOUND -> format(MessageKeys.SLIP_NOT_FOUND, language);
            case BANK_PENDING -> format(MessageKeys.SLIP_BANK_PENDING, language);
            case PROVIDER_UNAVAILABLE -> format(MessageKeys.SLIP_PROVIDER_UNAVAILABLE, language);
            case QUOTA_EXCEEDED -> format(MessageKeys.SLIP_QUOTA_EXCEEDED, language);
            case INVALID_IMAGE -> format(MessageKeys.SLIP_INVALID_IMAGE, language);
            case NOT_LINKED -> format(MessageKeys.CHAT_NOT_LINKED, language);
        };
    }

    /** Format the daily summary message (summary.daily). */
    public String dailySummary(String date, long verified, BigDecimal totalBaht, long review,
                               long duplicates, long rejected, String language) {
        return format(MessageKeys.SUMMARY_DAILY, language, date, String.valueOf(verified),
                amount(totalBaht), String.valueOf(review), String.valueOf(duplicates),
                String.valueOf(rejected));
    }

    public static String amount(BigDecimal baht) {
        return baht == null ? "-" : AMOUNT.format(baht);
    }

    public static String dateTime(Instant instant, ZoneId zone) {
        if (instant == null) {
            return "-";
        }
        return DATE_TIME.format(instant.atZone(zone == null ? FALLBACK_ZONE : zone));
    }

    public static Locale locale(String language) {
        if (language == null || language.isBlank()) {
            return Locale.ENGLISH;
        }
        return switch (language) {
            case "th" -> Locale.of("th", "TH");
            case "my" -> Locale.of("my");
            default -> Locale.ENGLISH;
        };
    }
}
