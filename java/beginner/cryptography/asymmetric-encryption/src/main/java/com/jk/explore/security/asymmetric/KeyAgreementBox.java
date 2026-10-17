package com.jk.explore.security.asymmetric;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.KDF;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.HKDFParameterSpec;

/**
 * Key agreement with X25519 (elliptic-curve Diffie-Hellman). Each side makes a key pair and sends only its public key. Each
 * combines its own private key with the other's public key and gets the same secret, which neither side ever sent. The secret
 * is then run through HKDF to make the key to use.
 */
public final class KeyAgreementBox {

    private KeyAgreementBox() {
    }

    public static KeyPair newKeyPair() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The raw shared secret from one side's private key and the other side's public key. */
    public static byte[] sharedSecret(PrivateKey mine, PublicKey theirs) {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance("X25519");
            agreement.init(mine);
            agreement.doPhase(theirs, true);
            return agreement.generateSecret();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A 32-byte key made from the shared secret with HKDF-SHA-256, tied to a purpose by {@code info}. */
    public static byte[] deriveKey(byte[] sharedSecret, byte[] salt, byte[] info) {
        return hkdf(sharedSecret, salt, info, 32);
    }

    /** HKDF (RFC 5869) with SHA-256: extract a key from the input, then expand it to {@code length} bytes. */
    public static byte[] hkdf(byte[] inputKeyMaterial, byte[] salt, byte[] info, int length) {
        try {
            KDF kdf = KDF.getInstance("HKDF-SHA256");
            return kdf.deriveData(HKDFParameterSpec.ofExtract().addIKM(inputKeyMaterial).addSalt(salt).thenExpand(info, length));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
