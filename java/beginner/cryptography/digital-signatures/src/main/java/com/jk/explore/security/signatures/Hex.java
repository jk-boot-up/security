package com.jk.explore.security.signatures;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Small helpers for text and bytes. */
final class Hex {

    private Hex() {
    }

    static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static String of(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    static byte[] parse(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
