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

    /** Bytes from..to as hex in groups of four bytes: "a83b53e9 ba4d832c ...". */
    static String grouped(byte[] bytes, int from, int to) {
        return of(java.util.Arrays.copyOfRange(bytes, from, to)).replaceAll("(.{8})(?!$)", "$1 ");
    }

    static byte[] parse(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
