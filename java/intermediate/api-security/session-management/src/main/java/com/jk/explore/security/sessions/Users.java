package com.jk.explore.security.sessions;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** The bank's customers and their passwords, kept as salted PBKDF2 hashes and checked in constant time. */
final class Users {

    private static final int ROUNDS = 100_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Stored(byte[] salt, byte[] hash) {
    }

    private final Map<String, Stored> users = new HashMap<>();
    private final Stored unknown = store(new char[] {'x'});

    synchronized void set(String user, char[] password) {
        users.put(user, store(password));
    }

    /** An unknown name costs the same hash as a known one, so the answer's timing says nothing about which names exist. */
    synchronized boolean check(String user, char[] password) {
        Stored stored = users.getOrDefault(user, unknown);
        boolean match = MessageDigest.isEqual(stored.hash(), derive(password, stored.salt()));
        return match && users.containsKey(user);
    }

    private static Stored store(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return new Stored(salt, derive(password, salt));
    }

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ROUNDS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is part of every Java runtime", e);
        } finally {
            spec.clearPassword();
        }
    }
}
