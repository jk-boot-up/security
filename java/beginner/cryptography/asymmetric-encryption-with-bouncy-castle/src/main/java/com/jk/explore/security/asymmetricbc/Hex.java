package com.jk.explore.security.asymmetricbc;

import java.nio.charset.StandardCharsets;

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
}
