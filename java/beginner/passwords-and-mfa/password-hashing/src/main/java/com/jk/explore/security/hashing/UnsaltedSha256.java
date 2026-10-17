package com.jk.explore.security.hashing;

/**
 * A fast hash with nothing added. It hides the password, but the same password always gives the same
 * stored text, and a table of hashes of common passwords can be made once and reused against every account.
 */
public class UnsaltedSha256 implements PasswordHasher {

    private final WorkMeter meter;

    public UnsaltedSha256(WorkMeter meter) {
        this.meter = meter;
    }

    @Override
    public String name() {
        return "SHA-256, no salt";
    }

    @Override
    public String hash(String password) {
        meter.add(stepsPerHash());
        return Hex.sha256(password);
    }

    @Override
    public boolean verify(String password, String stored) {
        return Hex.sameText(hash(password), stored);
    }

    @Override
    public long stepsPerHash() {
        return 1;
    }
}
