package com.jk.explore.security.jwt;

/**
 * Base64url by hand, for teaching: three bytes are 24 bits, cut into four groups of 6 bits, and each group picks one of 64
 * characters. A test proves it gives the library's exact result.
 */
final class Base64UrlSteps {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private Base64UrlSteps() {
    }

    /** Encodes without padding, 6 bits at a time. */
    static String encode(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        int bits = 0;
        int count = 0;
        for (byte b : bytes) {
            bits = (bits << 8) | (b & 0xff);
            count += 8;
            while (count >= 6) {
                count -= 6;
                out.append(ALPHABET.charAt((bits >> count) & 0x3f));
            }
        }
        if (count > 0) {
            out.append(ALPHABET.charAt((bits << (6 - count)) & 0x3f));
        }
        return out.toString();
    }

    /** The four 6-bit groups of three bytes, as binary text: "011110 110010 001001 100001". */
    static String groups(byte[] three) {
        int bits = ((three[0] & 0xff) << 16) | ((three[1] & 0xff) << 8) | (three[2] & 0xff);
        StringBuilder out = new StringBuilder();
        for (int shift = 18; shift >= 0; shift -= 6) {
            String group = Integer.toBinaryString((bits >> shift) & 0x3f);
            out.append(shift == 18 ? "" : " ").append("0".repeat(6 - group.length())).append(group);
        }
        return out.toString();
    }
}
