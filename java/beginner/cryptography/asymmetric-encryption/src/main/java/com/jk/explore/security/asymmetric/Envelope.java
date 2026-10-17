package com.jk.explore.security.asymmetric;

/** A hybrid-encrypted message: the data key locked with RSA, the AES-GCM nonce, and the cipher text with its tag. */
public record Envelope(byte[] lockedKey, byte[] nonce, byte[] ciphertextWithTag) {

    public int totalLength() {
        return lockedKey.length + nonce.length + ciphertextWithTag.length;
    }
}
