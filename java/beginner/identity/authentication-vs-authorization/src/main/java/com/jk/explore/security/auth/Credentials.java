package com.jk.explore.security.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Checks a username and password. Passwords are never stored: only a slow, salted hash of each one is
 * kept. The salt here is derived from the username so the demo is repeatable; a real system uses a random
 * salt per account. How and why hashing works is the subject of the password-hashing project.
 */
public class Credentials {

    static final int ITERATIONS = 10_000;

    private final Map<String, byte[]> hashes = new HashMap<>();

    public Credentials add(String username, String password) {
        hashes.put(username, hash(username, password));
        return this;
    }

    /** True only if the account exists and the password matches. */
    public boolean verify(String username, String password) {
        byte[] stored = hashes.get(username);
        // Hash even for an unknown user, so a wrong username takes as long as a wrong password.
        byte[] attempt = hash(username, password);
        return stored != null && MessageDigest.isEqual(stored, attempt);
    }

    /** The hash kept for an account: 32 bytes, or null if there is no such account. */
    public byte[] storedHash(String username) {
        byte[] stored = hashes.get(username);
        return stored == null ? null : stored.clone();
    }

    /** The salt mixed into each hash. Derived from the username so the demo is repeatable; real salts are random. */
    static String salt(String username) {
        return "demo-salt:" + username;
    }

    /** PBKDF2: the password and the salt, hashed 10,000 times with HMAC-SHA256, gives 256 bits. */
    static byte[] hash(String username, String password) {
        try {
            byte[] salt = salt(username).getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is part of every Java runtime", e);
        }
    }
}
