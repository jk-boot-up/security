package com.jk.explore.security.keystore;

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

    static byte[] parse(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    /** True if {@code needle} appears anywhere inside {@code haystack}. */
    static boolean contains(byte[] haystack, byte[] needle) {
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            boolean same = true;
            for (int j = 0; j < needle.length && same; j++) {
                same = haystack[i + j] == needle[j];
            }
            if (same) {
                return true;
            }
        }
        return false;
    }
}
