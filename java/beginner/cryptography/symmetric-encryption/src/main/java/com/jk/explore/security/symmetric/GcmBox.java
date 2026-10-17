package com.jk.explore.security.symmetric;

import java.security.GeneralSecurityException;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES in GCM mode, an authenticated encryption: it hides the data and also adds a tag, so that any change to the cipher text,
 * the nonce or the associated data is noticed when the message is opened. The associated data is text that is not hidden but is
 * bound to the message, such as the account it belongs to.
 */
public class GcmBox {

    public static final int TAG_BYTES = 16;

    private final byte[] key;
    private final NonceSource nonces;

    public GcmBox(byte[] key, NonceSource nonces) {
        this.key = key.clone();
        this.nonces = nonces;
    }

    public Sealed seal(byte[] associatedData, byte[] plaintext) {
        byte[] nonce = nonces.next();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce));
            cipher.updateAAD(associatedData);
            return new Sealed(nonce, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens a message, or throws {@link TamperedException} if anything about it has changed. */
    public byte[] open(byte[] associatedData, Sealed sealed) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, sealed.nonce()));
            cipher.updateAAD(associatedData);
            return cipher.doFinal(sealed.ciphertextWithTag());
        } catch (AEADBadTagException e) {
            throw new TamperedException("the tag does not match: wrong key, changed message, or wrong associated data");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Thrown when a message does not open. */
    public static class TamperedException extends RuntimeException {
        public TamperedException(String message) {
            super(message);
        }
    }
}
