package com.jk.explore.security.sessions;

import java.util.ArrayList;
import java.util.List;

/**
 * Redoes base64url by hand, so the demo can show how 16 bytes become 22 characters: the bytes are read as one long row of bits, the row is
 * cut into groups of 6 bits, and each group, a number from 0 to 63, picks one character of the alphabet. A test proves that the result is the
 * same as {@link SessionIds#encode}.
 */
final class Base64Steps {

    private Base64Steps() {
    }

    /** The bytes as 8-bit groups, for example "01001111". */
    static List<String> byteBits(byte[] bytes) {
        List<String> out = new ArrayList<>();
        for (byte b : bytes) {
            out.add(String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0'));
        }
        return out;
    }

    /** The same bits cut into 6-bit groups; the last group is filled up with zeros. */
    static List<String> sixBitGroups(byte[] bytes) {
        String bits = String.join("", byteBits(bytes));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < bits.length(); i += 6) {
            String group = bits.substring(i, Math.min(i + 6, bits.length()));
            out.add(group + "0".repeat(6 - group.length()));
        }
        return out;
    }

    /** One character for each 6-bit group. */
    static String encode(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (String group : sixBitGroups(bytes)) {
            out.append(SessionIds.ALPHABET.charAt(Integer.parseInt(group, 2)));
        }
        return out.toString();
    }
}
