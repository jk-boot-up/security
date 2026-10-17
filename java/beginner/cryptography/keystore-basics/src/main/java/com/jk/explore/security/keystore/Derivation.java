package com.jk.explore.security.keystore;

import java.security.GeneralSecurityException;
import javax.crypto.KDF;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.PBEKeySpec;

/**
 * Making keys from other secrets. PBKDF2 turns a password and a salt into a key, slowly, so that guessing the password is
 * expensive. HKDF turns one strong master key into any number of separate keys, one for each purpose.
 */
public final class Derivation {

    /** The number of rounds that OWASP recommends for PBKDF2 with HMAC-SHA-256. */
    public static final int PBKDF2_ROUNDS = 600_000;

    private Derivation() {
    }

    public static byte[] fromPassword(char[] password, byte[] salt, int rounds) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** One key per purpose from a master key: the purpose text, called {@code info}, makes each key different. */
    public static byte[] forPurpose(byte[] masterKey, byte[] salt, String purpose) {
        try {
            KDF kdf = KDF.getInstance("HKDF-SHA256");
            return kdf.deriveData(HKDFParameterSpec.ofExtract().addIKM(masterKey).addSalt(salt).thenExpand(Hex.bytes(purpose), 32));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
