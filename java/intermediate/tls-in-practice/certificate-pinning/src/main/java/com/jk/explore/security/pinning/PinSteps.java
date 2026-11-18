package com.jk.explore.security.pinning;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.HexFormat;

/**
 * The pin of a key, worked out step by step: the key's SubjectPublicKeyInfo bytes, their SHA-256 hash, and the hash in Base64, with the first Base64 group
 * done by hand. A test proves the result is exactly {@link Spki#pin}.
 */
record PinSteps(byte[] spki, byte[] hash, String pin) {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    static PinSteps of(PublicKey key) {
        try {
            byte[] spki = key.getEncoded();
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(spki);
            return new PinSteps(spki, hash, base64(hash));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** For an EC P-256 key: a 26-byte header naming the algorithm and the curve, then the 65-byte point (04, x, y). */
    int headerBytes() {
        return spki.length - 65;
    }

    String hashHex() {
        return HexFormat.of().formatHex(hash);
    }

    /** Base64 by hand: every 3 bytes (24 bits) become 4 letters of 6 bits; a last group of 2 bytes becomes 3 letters and one '='. */
    static String base64(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 3) {
            int n = Math.min(3, bytes.length - i);
            int bits = 0;
            for (int j = 0; j < 3; j++) {
                bits = bits << 8 | (j < n ? bytes[i + j] & 0xff : 0);
            }
            for (int k = 0; k < 4; k++) {
                out.append(k <= n ? ALPHABET.charAt(bits >> 18 - 6 * k & 0x3f) : '=');
            }
        }
        return out.toString();
    }

    /** The first three bytes of the hash as four 6-bit values. */
    int[] firstGroup() {
        int bits = (hash[0] & 0xff) << 16 | (hash[1] & 0xff) << 8 | hash[2] & 0xff;
        return new int[] {bits >> 18 & 0x3f, bits >> 12 & 0x3f, bits >> 6 & 0x3f, bits & 0x3f};
    }
}
