package com.jk.explore.security.sessions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Session ids: 128 bits from SecureRandom, written as 22 base64url characters. An id is built from nothing but randomness, so it says
 * nothing about the user or the time. The server stores only the SHA-256 of each id, so a copy of the session store holds no live ids.
 */
final class SessionIds {

    static final int BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionIds() {
    }

    static String newId() {
        byte[] bytes = new byte[BITS / 8];
        RANDOM.nextBytes(bytes);
        return encode(bytes);
    }

    /** Writes bytes as base64url without padding: every 6 bits become one of 64 characters, so 16 bytes become 22 characters. */
    static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The base64url alphabet: the character for each 6-bit value, 0 to 63. */
    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    static byte[] hash(String id) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(id.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    /** The key under which a session is stored: the hash of its id, as hex. */
    static String key(byte[] hash) {
        return HexFormat.of().formatHex(hash);
    }
}
