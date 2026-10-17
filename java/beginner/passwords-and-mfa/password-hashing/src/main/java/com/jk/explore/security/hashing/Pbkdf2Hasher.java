package com.jk.explore.security.hashing;

import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A slow, salted hash: PBKDF2 with HMAC-SHA-256, repeated {@code iterations} times. The stored text holds
 * everything needed to check a password again: {@code pbkdf2-sha256$iterations$salt$hash}. Because the
 * iteration count is stored with each hash, the cost can be raised later without breaking old accounts.
 */
public class Pbkdf2Hasher implements PasswordHasher {

    private static final String PREFIX = "pbkdf2-sha256";

    private final WorkMeter meter;
    private final SaltSource salts;
    private final int iterations;

    public Pbkdf2Hasher(WorkMeter meter, SaltSource salts, int iterations) {
        this.meter = meter;
        this.salts = salts;
        this.iterations = iterations;
    }

    @Override
    public String name() {
        return "PBKDF2, salted, " + iterations + " rounds";
    }

    @Override
    public String hash(String password) {
        return hashWith(password, salts.next(), iterations);
    }

    @Override
    public boolean verify(String password, String stored) {
        String[] parts = stored.split("\\$");
        int storedIterations = Integer.parseInt(parts[1]);
        byte[] salt = HexFormat.of().parseHex(parts[2]);
        return Hex.sameText(hashWith(password, salt, storedIterations), stored);
    }

    /** True if this hash was made with fewer rounds than are used today. */
    public boolean needsUpgrade(String stored) {
        return iterationsOf(stored) < iterations;
    }

    public static int iterationsOf(String stored) {
        return Integer.parseInt(stored.split("\\$")[1]);
    }

    /** A hash made with a chosen, possibly old, number of rounds: how an account from the past looks. */
    public String hashWithRounds(String password, int rounds) {
        return hashWith(password, salts.next(), rounds);
    }

    private String hashWith(String password, byte[] salt, int rounds) {
        meter.add(rounds);
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return PREFIX + "$" + rounds + "$" + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is part of every Java runtime", e);
        }
    }

    @Override
    public long stepsPerHash() {
        return iterations;
    }

    public int iterations() {
        return iterations;
    }
}
