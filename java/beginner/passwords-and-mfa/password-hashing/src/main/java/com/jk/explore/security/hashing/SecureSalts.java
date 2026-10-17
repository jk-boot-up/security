package com.jk.explore.security.hashing;

import java.security.SecureRandom;

/** Real salts: random bytes from the system's secure random generator. Use this one in production. */
public class SecureSalts implements SaltSource {

    private final SecureRandom random = new SecureRandom();

    @Override
    public byte[] next() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
}
