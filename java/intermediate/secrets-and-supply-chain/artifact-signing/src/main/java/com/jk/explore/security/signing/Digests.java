package com.jk.explore.security.signing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 digests, written as hex, and compared in constant time. */
final class Digests {

    private Digests() {
    }

    static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] part : parts) {
                digest.update(part);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    static byte[] unhex(String text) {
        return HexFormat.of().parseHex(text);
    }

    /** The first sixteen hex digits, which is enough to tell digests apart on screen. */
    static String shortHex(byte[] bytes) {
        return hex(bytes).substring(0, 16);
    }

    /** Equal digests, compared in time that does not depend on where they first differ. */
    static boolean same(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
