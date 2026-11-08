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

    /** The text as UTF-16, the way a PKCS12 file stores a key's name (its friendlyName). */
    static byte[] utf16(String text) {
        return text.getBytes(StandardCharsets.UTF_16BE);
    }

    static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static String of(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    /** The first 16 bytes in groups of four, "68617264 2d636f64 ...", and "..." if there are more. */
    static String first16(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(16, bytes.length); i += 4) {
            out.append(i == 0 ? "" : " ").append(of(java.util.Arrays.copyOfRange(bytes, i, Math.min(i + 4, bytes.length))));
        }
        return out + (bytes.length > 16 ? " ..." : "");
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
