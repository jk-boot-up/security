package com.jk.explore.security.asymmetricbc;

import java.util.ArrayList;
import java.util.List;

/**
 * Base64 by hand, the way PEM turns a key's bytes into text: every 3 bytes (24 bits) are cut into four 6-bit groups, and each
 * group, a number from 0 to 63, picks one letter from the alphabet below. The library does this inside its PEM writer; this
 * class redoes it so that the steps can be shown, and a test proves it gives the library's exact text.
 */
public final class Base64Steps {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private Base64Steps() {
    }

    /** The 24 bits of three bytes, as four 6-bit numbers from 0 to 63. */
    public static int[] groups(byte b1, byte b2, byte b3) {
        int bits = (b1 & 0xff) << 16 | (b2 & 0xff) << 8 | (b3 & 0xff);
        return new int[] {bits >> 18 & 63, bits >> 12 & 63, bits >> 6 & 63, bits & 63};
    }

    /** All the bytes as Base64 text, with "=" at the end when the length is not a multiple of three. */
    public static String encode(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 3) {
            int left = Math.min(3, bytes.length - i);
            int[] g = groups(bytes[i], left > 1 ? bytes[i + 1] : 0, left > 2 ? bytes[i + 2] : 0);
            for (int k = 0; k < 4; k++) {
                out.append(k <= left ? ALPHABET.charAt(g[k]) : '=');
            }
        }
        return out.toString();
    }

    /** The text cut into lines of at most 64 characters, as PEM does. */
    public static List<String> lines(String base64) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < base64.length(); i += 64) {
            lines.add(base64.substring(i, Math.min(base64.length(), i + 64)));
        }
        return lines;
    }

    /** A number as binary digits, padded to a width: a byte as 00110000, a group as 001100. */
    static String bits(int value, int width) {
        String s = Integer.toBinaryString(value);
        return "0".repeat(width - s.length()) + s;
    }
}
