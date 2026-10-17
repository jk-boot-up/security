package com.jk.explore.security.hashing;

/** The worst way to store a password: as it is. Anyone who reads the table reads every password. */
public class PlainText implements PasswordHasher {

    private final WorkMeter meter;

    public PlainText(WorkMeter meter) {
        this.meter = meter;
    }

    @Override
    public String name() {
        return "plain text";
    }

    @Override
    public String hash(String password) {
        return password;
    }

    @Override
    public boolean verify(String password, String stored) {
        meter.add(stepsPerHash());
        return password.equals(stored);
    }

    @Override
    public long stepsPerHash() {
        return 1;
    }
}
