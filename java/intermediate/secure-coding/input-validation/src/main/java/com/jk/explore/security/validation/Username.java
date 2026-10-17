package com.jk.explore.security.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A login name in its one canonical form. The order matters: limit the size, refuse invisible and control characters, normalise
 * (NFKC folds full-width and other compatibility forms into plain letters), trim, lower-case without the machine's locale, and only then
 * check the allow-list. Checking before normalising would let two spellings of one name pass as two names.
 */
public record Username(String value) {

    private static final Pattern ALLOWED = Pattern.compile("[a-z0-9][a-z0-9._-]{2,19}");

    public static Username parse(String raw) throws Rejected {
        String text = Text.bounded("username", raw, 64);
        if (Text.hasControlOrFormat(text)) {
            throw new Rejected("username", "no control or invisible characters");
        }
        String canonical = Text.nfkc(text).strip().toLowerCase(Locale.ROOT);
        if (!Text.asciiOnly(canonical) || !ALLOWED.matcher(canonical).matches()) {
            throw new Rejected("username", "3 to 20 letters, digits, dots, dashes or underscores");
        }
        return new Username(canonical);
    }
}
