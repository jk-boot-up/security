package com.jk.explore.security.webhooks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * HMAC-SHA256 worked out by hand, for teaching (RFC 2104): the key is padded to one 64-byte block, mixed with 0x36 for an inner hash and with
 * 0x5c for an outer hash. A test proves the result equals the JDK's {@code Mac}; the store itself uses {@link Signatures}.
 */
final class HmacSteps {

    static final int BLOCK = 64;

    /** Every value on the way to the HMAC. */
    record Steps(byte[] paddedKey, byte[] innerKey, byte[] inner, byte[] outerKey, byte[] mac) {
    }

    private HmacSteps() {
    }

    static Steps of(byte[] key, byte[] message) {
        byte[] padded = Arrays.copyOf(key.length > BLOCK ? sha256(key) : key, BLOCK);
        byte[] innerKey = xor(padded, 0x36);
        byte[] inner = sha256(concat(innerKey, message));
        byte[] outerKey = xor(padded, 0x5c);
        byte[] mac = sha256(concat(outerKey, inner));
        return new Steps(padded, innerKey, inner, outerKey, mac);
    }

    private static byte[] xor(byte[] block, int pad) {
        byte[] out = new byte[block.length];
        for (int i = 0; i < block.length; i++) {
            out[i] = (byte) (block[i] ^ pad);
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
