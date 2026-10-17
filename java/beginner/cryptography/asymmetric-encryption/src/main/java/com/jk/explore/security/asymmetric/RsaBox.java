package com.jk.explore.security.asymmetric;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * RSA encryption with OAEP padding. Anyone can lock a small message with the public key, and only the holder of the private key
 * can unlock it. OAEP adds randomness, so the same message gives a different result each time, and it must always be used: bare
 * ("textbook") RSA is broken in practice. Only short messages fit, so RSA is used to carry a key and not the data.
 */
public final class RsaBox {

    public static final int KEY_BITS = 2048;

    private RsaBox() {
    }

    public static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_BITS);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The most bytes that fit: the key size in bytes, minus twice the hash size, minus 2. For 2048 bits and SHA-256 that is 190. */
    public static int maxMessageBytes() {
        return KEY_BITS / 8 - 2 * 32 - 2;
    }

    public static byte[] lock(PublicKey publicKey, byte[] message) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaep());
            return cipher.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /** Opens a locked message, or throws {@link NotForThisKeyException} if this private key is not the one it was locked for. */
    public static byte[] unlock(PrivateKey privateKey, byte[] locked) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaep());
            return cipher.doFinal(locked);
        } catch (java.security.GeneralSecurityException e) {
            throw new NotForThisKeyException("this private key cannot open the message");
        }
    }

    private static OAEPParameterSpec oaep() {
        return new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }

    /** Thrown when a private key does not open a message. */
    public static class NotForThisKeyException extends RuntimeException {
        public NotForThisKeyException(String message) {
            super(message);
        }
    }
}
