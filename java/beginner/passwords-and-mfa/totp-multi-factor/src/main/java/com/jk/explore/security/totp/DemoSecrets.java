package com.jk.explore.security.totp;

import java.nio.charset.StandardCharsets;

/**
 * Fixed secrets, so the demo prints the same codes on every run. The secret is the key used in the published test
 * vectors of RFC 4226 and RFC 6238, which lets the codes be checked against the standards. It is public and must
 * never protect a real account.
 */
public class DemoSecrets implements SecretSource {

    public static final byte[] RFC_KEY = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    private int recovery;

    @Override
    public byte[] newSecret() {
        return RFC_KEY.clone();
    }

    @Override
    public String newRecoveryCode() {
        return "recovery-" + (++recovery);
    }
}
