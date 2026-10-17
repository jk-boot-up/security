package com.jk.explore.security.totp;

/**
 * TOTP (RFC 6238): HOTP where the counter is the number of whole steps of time since the start of 1970. With a step of
 * 30 seconds, the code changes every 30 seconds, and the phone and the server agree on it without talking to each other.
 */
public final class Totp {

    public static final int STEP_SECONDS = 30;
    public static final int DIGITS = 6;

    private Totp() {
    }

    public static long counterAt(long unixSeconds) {
        return unixSeconds / STEP_SECONDS;
    }

    public static String codeAt(byte[] key, long unixSeconds) {
        return Hotp.code(key, counterAt(unixSeconds), DIGITS);
    }

    public static String codeAt(byte[] key, long unixSeconds, int digits, String hmacAlgorithm) {
        return Hotp.code(key, counterAt(unixSeconds), digits, hmacAlgorithm);
    }
}
