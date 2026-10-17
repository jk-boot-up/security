package com.jk.explore.security.totp;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Real secrets and recovery codes, from the system's secure random generator. Use this one in production. */
public class SecureSecrets implements SecretSource {

    private final SecureRandom random = new SecureRandom();

    @Override
    public byte[] newSecret() {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        return secret;
    }

    @Override
    public String newRecoveryCode() {
        byte[] bytes = new byte[5];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
