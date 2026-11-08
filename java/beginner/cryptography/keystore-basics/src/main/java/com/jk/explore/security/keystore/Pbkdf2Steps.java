package com.jk.explore.security.keystore;

import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2 with HMAC-SHA-256 worked out by hand, for one 32-byte key, so the rounds can be seen. A test checks it gives exactly
 * the key that {@link Derivation#fromPassword} gets from the JDK.
 */
public final class Pbkdf2Steps {

    /** The first two rounds, and the key: every round's result XORed together. */
    public record Steps(byte[] u1, byte[] u2, byte[] key) {
    }

    private Pbkdf2Steps() {
    }

    public static Steps derive(String password, byte[] salt, int rounds) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(Hex.bytes(password), "HmacSHA256"));
            byte[] u = hmac.doFinal(concat(salt, new byte[] {0, 0, 0, 1}));
            byte[] u1 = u;
            byte[] u2 = null;
            byte[] key = u.clone();
            for (int round = 2; round <= rounds; round++) {
                u = hmac.doFinal(u);
                if (round == 2) {
                    u2 = u;
                }
                for (int i = 0; i < key.length; i++) {
                    key[i] ^= u[i];
                }
            }
            return new Steps(u1, u2, key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
