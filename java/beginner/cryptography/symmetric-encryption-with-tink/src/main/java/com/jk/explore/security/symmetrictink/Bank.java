package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.aead.AeadConfig;
import java.security.GeneralSecurityException;

/** The bank's encryption of statement lines with Tink's AEAD primitive: Tink picks the nonce, the layout and the mode. */
public class Bank {

    static {
        try {
            AeadConfig.register();
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Aead aead;

    public Bank(KeysetHandle keyset) {
        try {
            this.aead = keyset.getPrimitive(RegistryConfiguration.get(), Aead.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public byte[] seal(byte[] associatedData, byte[] plaintext) {
        try {
            return aead.encrypt(plaintext, associatedData);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens a message, or returns null if it does not open (wrong key, changed message or wrong associated data). */
    public byte[] openOrNull(byte[] associatedData, byte[] sealed) {
        try {
            return aead.decrypt(sealed, associatedData);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    /** The primitive itself, for storing a keyset encrypted with it. */
    public Aead aead() {
        return aead;
    }
}
