package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.Aead;
import java.security.GeneralSecurityException;

/** Wraps an Aead and counts how often it is used: stands for the calls a real key management service would be asked to make. */
public class CountingAead implements Aead {

    private final Aead inner;
    private int calls;

    public CountingAead(Aead inner) {
        this.inner = inner;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
        calls++;
        return inner.encrypt(plaintext, associatedData);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] associatedData) throws GeneralSecurityException {
        calls++;
        return inner.decrypt(ciphertext, associatedData);
    }

    public int calls() {
        return calls;
    }
}
