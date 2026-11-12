package com.slipcheck.backend.i18n;

/**
 * All message bundle keys. Centralised so no key is ever a bare string inline.
 * Every key must exist in messages_en/th/my.properties (enforced by MessageBundleTest).
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    public static final String SLIP_VERIFIED = "slip.verified";
    public static final String SLIP_AMOUNT_MISMATCH = "slip.amount_mismatch";
    public static final String SLIP_RECEIVER_UNCERTAIN = "slip.receiver_uncertain";
    public static final String SLIP_DUPLICATE = "slip.duplicate";
    public static final String SLIP_WRONG_RECEIVER = "slip.wrong_receiver";
    public static final String SLIP_UNREADABLE_QR = "slip.unreadable_qr";
    public static final String SLIP_NOT_FOUND = "slip.not_found";
    public static final String SLIP_BANK_PENDING = "slip.bank_pending";
    public static final String SLIP_PROVIDER_UNAVAILABLE = "slip.provider_unavailable";
    public static final String SLIP_QUOTA_EXCEEDED = "slip.quota_exceeded";
    public static final String SLIP_INVALID_IMAGE = "slip.invalid_image";

    public static final String CHAT_WELCOME = "chat.welcome";
    public static final String CHAT_NOT_LINKED = "chat.not_linked";
    public static final String CHAT_LINKED = "chat.linked";
    public static final String CHAT_UNLINKED = "chat.unlinked";
    public static final String CHAT_LINK_FAILED = "chat.link_failed";
    public static final String CHAT_LANGUAGE_PROMPT = "chat.language_prompt";
    public static final String CHAT_LANGUAGE_SET = "chat.language_set";
    public static final String CHAT_HELP = "chat.help";

    public static final String SUMMARY_DAILY = "summary.daily";
    public static final String SUMMARY_EMPTY = "summary.empty";
}
