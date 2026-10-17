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

    private static final int ITERATIONS = 10_000;

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

    private static byte[] hash(String username, String password) {
        try {
            byte[] salt = ("demo-salt:" + username).getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is part of every Java runtime", e);
        }
    }
}
