package com.jk.explore.security.validation;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A name shown to other people, where letters from every script are welcome. It is stored in NFC, so a name typed as e plus a
 * combining accent and a name typed with the single accented letter are the same name. Because many scripts have letters that look like
 * Latin ones, a display name is also compared by its skeleton, so that nobody can appear as the shop's own staff.
 */
public record DisplayName(String value) {

    /** A few letters from other scripts that look like Latin letters (a small part of the Unicode confusables list). */
    private static final Map<Integer, Character> LOOKALIKES = Map.of(
            0x0430, 'a', 0x0435, 'e', 0x043E, 'o', 0x0440, 'p', 0x0441, 'c',
            0x0456, 'i', 0x03BF, 'o', 0x0410, 'a', 0x0405, 's', 0x0455, 's');

    private static final Set<String> STAFF = Set.of("shop support", "ana");

    public static DisplayName parse(String raw) throws Rejected {
        String text = Text.bounded("display name", raw, 80);
        if (Text.hasControlOrFormat(text)) {
            throw new Rejected("display name", "no control or invisible characters");
        }
        String nfc = Text.nfc(text).strip();
        if (nfc.isEmpty() || nfc.codePointCount(0, nfc.length()) > 40) {
            throw new Rejected("display name", "1 to 40 characters");
        }
        if (STAFF.contains(skeleton(nfc))) {
            throw new Rejected("display name", "reserved for the shop's staff");
        }
        return new DisplayName(nfc);
    }

    /** The text with look-alike letters replaced by the Latin letter they imitate, compatibility forms folded and case removed. */
    public static String skeleton(String text) {
        StringBuilder out = new StringBuilder();
        Text.nfkc(text).toLowerCase(Locale.ROOT).codePoints()
                .forEach(c -> out.append(LOOKALIKES.containsKey(c) ? String.valueOf(LOOKALIKES.get(c)) : Character.toString(c)));
        return out.toString();
    }
}
