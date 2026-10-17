package com.jk.explore.security.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An e-mail address for receipts. Deliberately narrower than everything RFC 5321 allows: a store needs addresses that work, not every
 * address that can be written. The domain is lower-cased without the machine's locale; the local part is kept as typed.
 */
public record Email(String value) {

    public static final int MAX = 254;
    private static final Pattern LOCAL = Pattern.compile("[A-Za-z0-9._%+-]{1,64}");
    private static final Pattern LABEL = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    public static Email parse(String raw) throws Rejected {
        String text = Text.bounded("email", raw, MAX);
        int at = text.indexOf('@');
        if (at <= 0 || at != text.lastIndexOf('@') || at == text.length() - 1) {
            throw new Rejected("email", "not a valid address");
        }
        String local = text.substring(0, at);
        String domain = text.substring(at + 1).toLowerCase(Locale.ROOT);
        if (!LOCAL.matcher(local).matches() || local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            throw new Rejected("email", "not a valid address");
        }
        String[] labels = domain.split("\\.", -1);
        if (labels.length < 2) {
            throw new Rejected("email", "not a valid address");
        }
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) {
                throw new Rejected("email", "not a valid address");
            }
        }
        return new Email(local + "@" + domain);
    }
}
