package com.jk.explore.security.symmetric;

/** An encrypted message: the nonce that was used, and the cipher text with its 16-byte authentication tag at the end. */
public record Sealed(byte[] nonce, byte[] ciphertextWithTag) {

    /** The cipher text without the tag. */
    public byte[] ciphertext() {
        return java.util.Arrays.copyOf(ciphertextWithTag, ciphertextWithTag.length - GcmBox.TAG_BYTES);
    }

    /** The whole message as it would be stored: nonce first, then cipher text and tag. */
    public int storedLength() {
        return nonce.length + ciphertextWithTag.length;
    }
}
