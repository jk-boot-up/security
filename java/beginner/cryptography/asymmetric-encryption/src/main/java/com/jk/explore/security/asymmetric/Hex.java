package com.jk.explore.security.asymmetric;

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

    /** The first {@code count} bytes as hex in groups of four, and "..." when there are more. */
    static String start(byte[] bytes, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(count, bytes.length); i += 4) {
            out.append(i == 0 ? "" : " ").append(of(java.util.Arrays.copyOfRange(bytes, i, Math.min(i + 4, bytes.length))));
        }
        return bytes.length > count ? out + " ..." : out.toString();
    }

    static byte[] parse(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
