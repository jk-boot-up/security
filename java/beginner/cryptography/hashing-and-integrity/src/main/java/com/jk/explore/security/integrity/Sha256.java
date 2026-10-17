package com.jk.explore.security.integrity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256: a cryptographic hash. Any input, of any size, gives 32 bytes, and no one can find another input with the same result. */
public final class Sha256 {

    private Sha256() {
    }

    public static byte[] digest(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    public static String hex(byte[] data) {
        return Hex.of(digest(data));
    }

    public static String hex(String text) {
        return hex(Hex.bytes(text));
    }
}
