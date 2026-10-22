package com.jk.explore.security.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds the shapes of secrets in text that was already written, and covers them. A safety net: it knows only the shapes it was given. */
public final class Masker {

    private static final Pattern CARD = Pattern.compile("(?<![\\d-])(?:\\d[ -]?){12,18}\\d(?![\\d-])");
    private static final Pattern PAIR = Pattern.compile("(?i)(\"?(?:password|passwd|pwd|secret|api[_-]?key|token|cvv)\"?\\s*[:=]\\s*\"?)(\\[REDACTED\\]|[^\\s&\",}\\]]+)");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern BEARER = Pattern.compile("(?i)\\b(Bearer\\s+)[A-Za-z0-9._~+/=-]{8,}");
    private static final Pattern URL_PASSWORD = Pattern.compile("(://[^:/\\s@]+:)([^@\\s]+)(@)");

    private Masker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        String out = text;
        out = URL_PASSWORD.matcher(out).replaceAll("$1[REDACTED]$3");
        out = BEARER.matcher(out).replaceAll("$1[REDACTED]");
        out = JWT.matcher(out).replaceAll("[REDACTED]");
        out = PAIR.matcher(out).replaceAll("$1[REDACTED]");
        out = cards(out);
        return out;
    }

    /** A run of 13 to 19 digits is a card only if the Luhn check passes; an order number that happens to be long is left alone. */
    static String cards(String text) {
        Matcher m = CARD.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            m.appendReplacement(out, Matcher.quoteReplacement(luhn(digits) ? "[CARD ****" + digits.substring(digits.length() - 4) + "]" : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean second = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (second) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            second = !second;
        }
        return sum % 10 == 0;
    }
}
