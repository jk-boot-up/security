package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Constant-time comparison and SHA-256, the two byte operations every check here relies on. */
final class Bytes {

    private Bytes() {
    }

    /** Compares two strings in time that does not depend on where they first differ. */
    static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    static String sha256Base64Url(String text) {
        return Base64Url.encode(sha256(text.getBytes(StandardCharsets.US_ASCII)));
    }
}
