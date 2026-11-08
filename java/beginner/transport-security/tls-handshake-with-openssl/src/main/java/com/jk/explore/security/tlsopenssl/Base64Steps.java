package com.jk.explore.security.tlsopenssl;

/**
 * Base64 worked out by hand for one group of three bytes, so the demo can show how PEM text is made: three bytes are 24 bits, cut into
 * four groups of 6 bits, and each group (0 to 63) is a letter of the alphabet below. A test proves it matches the JDK's encoder.
 */
final class Base64Steps {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private Base64Steps() {
    }

    /** The four 6-bit groups of three bytes. */
    static int[] groups(byte[] three) {
        int bits = (three[0] & 0xff) << 16 | (three[1] & 0xff) << 8 | (three[2] & 0xff);   // 24 bits
        return new int[] {bits >> 18 & 63, bits >> 12 & 63, bits >> 6 & 63, bits & 63};
    }

    /** The four Base64 characters of three bytes. */
    static String encode(byte[] three) {
        StringBuilder out = new StringBuilder();
        for (int group : groups(three)) {
            out.append(ALPHABET.charAt(group));                                             // 0 = A ... 63 = /
        }
        return out.toString();
    }

    /** A number as binary digits, padded to the given width. */
    static String binary(int value, int width) {
        String digits = Integer.toBinaryString(value);
        return "0".repeat(width - digits.length()) + digits;
    }
}
