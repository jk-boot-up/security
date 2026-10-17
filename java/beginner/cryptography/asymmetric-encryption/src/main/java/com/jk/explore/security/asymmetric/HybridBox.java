package com.jk.explore.security.asymmetric;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Hybrid encryption: the data is encrypted with a fresh AES-256-GCM key made for this one message, and only that small key is
 * locked with the recipient's RSA public key. AES does the heavy work, and RSA solves the problem of sharing the key.
 */
public final class HybridBox {

    private static final SecureRandom RANDOM = new SecureRandom();

    private HybridBox() {
    }

    public static Envelope seal(PublicKey recipient, byte[] associatedData, byte[] plaintext) {
        byte[] dataKey = new byte[32];
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(dataKey);
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(associatedData);
            return new Envelope(RsaBox.lock(recipient, dataKey), nonce, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens an envelope with the recipient's private key, or throws if the key, the message or the associated data is wrong. */
    public static byte[] open(PrivateKey recipient, byte[] associatedData, Envelope envelope) {
        byte[] dataKey = RsaBox.unlock(recipient, envelope.lockedKey());
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new GCMParameterSpec(128, envelope.nonce()));
            cipher.updateAAD(associatedData);
            return cipher.doFinal(envelope.ciphertextWithTag());
        } catch (javax.crypto.AEADBadTagException e) {
            throw new RsaBox.NotForThisKeyException("the message was changed, or the associated data is wrong");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
