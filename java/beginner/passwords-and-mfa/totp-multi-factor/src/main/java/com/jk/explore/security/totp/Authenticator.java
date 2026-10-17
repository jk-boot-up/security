package com.jk.explore.security.totp;

/** The authenticator app on the customer's phone: it holds the secret and works out the code from its own clock. */
public class Authenticator {

    private final byte[] secret;
    private final long clockOffsetSeconds;

    public Authenticator(byte[] secret, long clockOffsetSeconds) {
        this.secret = secret.clone();
        this.clockOffsetSeconds = clockOffsetSeconds;
    }

    /** The code the phone shows when the true time is {@code trueTime}; a phone with a wrong clock shows another code. */
    public String codeShownAt(long trueTime) {
        return Totp.codeAt(secret, trueTime + clockOffsetSeconds);
    }
}
