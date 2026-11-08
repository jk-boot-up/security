package com.jk.explore.security.signaturesbc;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Small helpers for text and bytes. */
final class Hex {

    private Hex() {
    }

    static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static String of(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    /** A 32-byte number as two lines of four groups of eight hex digits, so it fits on a slide. */
    static String[] lines(java.math.BigInteger value) {
        String h = String.format("%064x", value);
        return new String[] {groups(h.substring(0, 32)), groups(h.substring(32))};
    }

    private static String groups(String hex) {
        return String.join(" ", hex.substring(0, 8), hex.substring(8, 16), hex.substring(16, 24), hex.substring(24));
    }

    static byte[] parse(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
