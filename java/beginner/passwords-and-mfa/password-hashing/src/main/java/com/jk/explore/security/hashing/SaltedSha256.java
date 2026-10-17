package com.jk.explore.security.hashing;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * A fast hash with a different salt for every account. The same password now gives different stored text,
 * and a table made in advance no longer works. But each guess is still one cheap step, so an attacker can
 * still try millions of guesses against a single account.
 */
public class SaltedSha256 implements PasswordHasher {

    private final WorkMeter meter;
    private final SaltSource salts;

    public SaltedSha256(WorkMeter meter, SaltSource salts) {
        this.meter = meter;
        this.salts = salts;
    }

    @Override
    public String name() {
        return "SHA-256, salted";
    }

    @Override
    public String hash(String password) {
        return withSalt(HexFormat.of().formatHex(salts.next()), password);
    }

    @Override
    public boolean verify(String password, String stored) {
        String saltHex = stored.substring(0, stored.indexOf('$'));
        return Hex.sameText(withSalt(saltHex, password), stored);
    }

    private String withSalt(String saltHex, String password) {
        meter.add(stepsPerHash());
        byte[] salt = HexFormat.of().parseHex(saltHex);
        byte[] text = password.getBytes(StandardCharsets.UTF_8);
        byte[] joined = new byte[salt.length + text.length];
        System.arraycopy(salt, 0, joined, 0, salt.length);
        System.arraycopy(text, 0, joined, salt.length, text.length);
        return saltHex + "$" + Hex.sha256(joined);
    }

    @Override
    public long stepsPerHash() {
        return 1;
    }
}
