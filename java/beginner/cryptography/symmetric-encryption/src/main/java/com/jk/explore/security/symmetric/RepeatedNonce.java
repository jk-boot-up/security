package com.jk.explore.security.symmetric;

/** A "nonce source" that gives the same nonce every time: the mistake this project warns about. */
public class RepeatedNonce implements NonceSource {

    private final byte[] nonce;

    public RepeatedNonce(byte[] nonce) {
        this.nonce = nonce.clone();
    }

    @Override
    public byte[] next() {
        return nonce.clone();
    }
}
