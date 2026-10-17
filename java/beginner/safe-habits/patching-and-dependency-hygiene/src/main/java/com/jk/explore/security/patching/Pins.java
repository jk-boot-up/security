package com.jk.explore.security.patching;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** A lock file in memory: the SHA-256 of each reviewed artifact, by name and version. Anything else is refused. */
final class Pins {

    enum Result { VERIFIED, MISMATCH, NOT_PINNED }

    private final Map<String, String> pinned = new TreeMap<>();

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    void pin(String ref, byte[] reviewedContent) {
        pinned.put(ref, sha256(reviewedContent));
    }

    String hashOf(String ref) {
        return pinned.get(ref);
    }

    int size() {
        return pinned.size();
    }

    Result verify(String ref, byte[] content) {
        String expected = pinned.get(ref);
        if (expected == null) {
            return Result.NOT_PINNED;
        }
        byte[] expectedBytes = HexFormat.of().parseHex(expected);
        byte[] actual = HexFormat.of().parseHex(sha256(content));
        return MessageDigest.isEqual(expectedBytes, actual) ? Result.VERIFIED : Result.MISMATCH;
    }
}
