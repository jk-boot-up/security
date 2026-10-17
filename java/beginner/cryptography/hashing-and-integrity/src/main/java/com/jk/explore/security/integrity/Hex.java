package com.jk.explore.security.integrity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Small helpers for turning bytes into text and comparing them. */
final class Hex {

    private Hex() {
    }

    static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static String of(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    /** How many of the characters of two equally long texts differ. */
    static int differingCharacters(String a, String b) {
        int n = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                n++;
            }
        }
        return n;
    }

    /** How many bits differ between two equally long byte arrays. */
    static int differingBits(byte[] a, byte[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            n += Integer.bitCount((a[i] ^ b[i]) & 0xFF);
        }
        return n;
    }
}
