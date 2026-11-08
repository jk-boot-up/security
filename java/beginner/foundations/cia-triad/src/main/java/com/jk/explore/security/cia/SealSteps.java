package com.jk.explore.security.cia;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * Works out an order's seal by hand, the way HMAC-SHA256 does it, so each step can be shown:
 * pad the key to 64 bytes, hash (key xor 36) followed by the order text, then hash (key xor 5c)
 * followed by that first hash. The result is exactly the seal that {@link Seal} gets from Java.
 */
public final class SealSteps {

    /** SHA-256 works on blocks of 64 bytes, so the key is padded with zero bytes to that size. */
    static final int BLOCK = 64;

    /** The key padded to 64 bytes, the key xor 36 and xor 5c, the first (inner) hash, and the seal. */
    public record Steps(int keyLength, byte[] paddedKey, byte[] innerKey, byte[] outerKey, byte[] inner, byte[] seal) {
    }

    private SealSteps() {
    }

    public static Steps of(String secret, String text) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);   // a key longer than 64 bytes would be hashed first
        byte[] padded = new byte[BLOCK];
        System.arraycopy(key, 0, padded, 0, key.length);
        byte[] innerKey = xor(padded, (byte) 0x36);
        byte[] outerKey = xor(padded, (byte) 0x5c);
        byte[] inner = sha256(innerKey, text.getBytes(StandardCharsets.UTF_8));
        byte[] seal = sha256(outerKey, inner);
        return new Steps(key.length, padded, innerKey, outerKey, inner, seal);
    }

    private static byte[] xor(byte[] data, byte with) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ with);
        }
        return out;
    }

    private static byte[] sha256(byte[] first, byte[] then) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(first);
            return sha.digest(then);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
