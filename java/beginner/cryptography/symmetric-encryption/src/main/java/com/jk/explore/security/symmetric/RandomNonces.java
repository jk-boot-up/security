package com.jk.explore.security.symmetric;

import java.security.SecureRandom;

/** Real nonces: 12 random bytes from the system's secure random generator. Use this one in production. */
public class RandomNonces implements NonceSource {

    private final SecureRandom random = new SecureRandom();

    @Override
    public byte[] next() {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        return nonce;
    }
}
