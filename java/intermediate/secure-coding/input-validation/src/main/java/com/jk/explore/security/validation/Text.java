package com.jk.explore.security.validation;

import java.text.Normalizer;

/** Small helpers shared by the value types: limits first, then canonical form, then character rules. */
final class Text {

    private Text() {
    }

    /** Refuses null, and anything longer than the limit, before any other work is done on it. */
    static String bounded(String field, String raw, int maxChars) throws Rejected {
        if (raw == null) {
            throw new Rejected(field, "missing");
        }
        if (raw.length() > maxChars) {
            throw new Rejected(field, "longer than " + maxChars + " characters");
        }
        return raw;
    }

    /** True if the text holds a control or invisible format character (line breaks, tabs, zero-width spaces, direction marks). */
    static boolean hasControlOrFormat(String text) {
        return text.codePoints().anyMatch(c -> {
            int type = Character.getType(c);
            return type == Character.CONTROL || type == Character.FORMAT;
        });
    }

    static String nfc(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    static String nfkc(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC);
    }

    static boolean asciiOnly(String text) {
        return text.chars().allMatch(c -> c < 0x80);
    }
}
