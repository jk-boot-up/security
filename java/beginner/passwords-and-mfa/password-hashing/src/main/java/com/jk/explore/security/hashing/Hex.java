package com.jk.explore.security.hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Small helpers shared by the hashers. */
final class Hex {

    private Hex() {
    }

    static String sha256(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    /** "a5a5a5a5a5a5..." -> "a5a5a5a5 a5a5..." : hex in groups of eight, easier to read. */
    static String grouped(String hex) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 8) {
            out.append(i == 0 ? "" : " ").append(hex, i, Math.min(i + 8, hex.length()));
        }
        return out.toString();
    }

    static String grouped(byte[] bytes) {
        return grouped(HexFormat.of().formatHex(bytes));
    }

    static byte[] xor(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }

    /** Compares two texts in time that does not depend on where they first differ. */
    static boolean sameText(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
