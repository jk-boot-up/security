package com.jk.explore.security.symmetric;

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

    /** Hex in groups of four bytes, easier to read and compare: "52454e54 20202020". */
    static String grouped(byte[] bytes) {
        String hex = of(bytes);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 8) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(hex, i, Math.min(i + 8, hex.length()));
        }
        return out.toString();
    }

    static byte[] xor(byte[] a, byte[] b, int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }
}
