package com.jk.explore.security.securitylogging;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides what of an event may be written down. Secrets are removed by the name of the field they arrive in, and card numbers and
 * bearer tokens are also caught by their shape, so one typed into a free-text note does not slip through.
 */
final class Redactor {

    static final String REMOVED = "[removed]";

    private static final Set<String> SECRET_NAMES = Set.of("password", "passcode", "pin", "otp", "secret", "token", "authorization",
            "cookie", "session", "session-id");

    /** 13 to 19 digits, optionally grouped by spaces or hyphens: the shape of a payment card number. */
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,18}\\d\\b");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");

    private Redactor() {
    }

    static Map<String, String> details(Map<String, String> details) {
        Map<String, String> out = new LinkedHashMap<>();
        details.forEach((name, value) -> out.put(name, isSecretName(name) ? REMOVED : text(value)));
        return out;
    }

    static boolean isSecretName(String name) {
        return SECRET_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    /** Masks secrets found by shape in any text: a card keeps only its last four digits, a token is removed. */
    static String text(String value) {
        String out = BEARER.matcher(value).replaceAll("Bearer " + REMOVED);
        out = JWT.matcher(out).replaceAll(REMOVED);
        Matcher card = CARD.matcher(out);
        StringBuilder masked = new StringBuilder();
        while (card.find()) {
            String digits = card.group().replaceAll("[ -]", "");
            card.appendReplacement(masked, "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4));
        }
        card.appendTail(masked);
        return masked.toString();
    }
}
