package com.jk.explore.security.leaks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks secrets in text that has already been written, as a last line of defence. It does three things: card numbers (digits with optional spaces or dashes
 * that pass the Luhn check), credentials in {@code key=value} and JSON pairs by key name, and bearer and JWT-shaped tokens.
 * It cannot know every secret, which is why the first defence is not to log them.
 */
public final class Redactor {

    private static final Pattern DIGIT_RUN = Pattern.compile("(?<![0-9])(?:[0-9][ -]?){12,18}[0-9](?![0-9])");
    private static final Pattern PAIR = Pattern.compile("(?i)([\"']?(?:password|passwd|pwd|secret|token|api[_-]?key|authorization|cvv|cvc|pin)[\"']?\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|(?:Bearer\\s+)?[^\\s,;&}]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{8,}");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*");
    private static final Pattern URL_PASSWORD = Pattern.compile("(?i)([a-z][a-z0-9+.-]*://[^\\s:/@]+:)([^\\s@/]+)(@)");

    public String redact(String text) {
        String out = URL_PASSWORD.matcher(text).replaceAll("$1[REDACTED]$3");
        out = PAIR.matcher(out).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + (m.group(2).startsWith("\"") ? "\"[REDACTED]\"" : "[REDACTED]")));
        out = BEARER.matcher(out).replaceAll("Bearer [REDACTED]");
        out = JWT.matcher(out).replaceAll("[REDACTED-JWT]");
        return maskCards(out);
    }

    private static String maskCards(String text) {
        Matcher m = DIGIT_RUN.matcher(text);
        StringBuilder b = new StringBuilder();
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            String replacement = digits.length() >= 13 && Luhn.valid(digits) ? "[CARD ****" + digits.substring(digits.length() - 4) + "]" : m.group();
            m.appendReplacement(b, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(b);
        return b.toString();
    }
}
