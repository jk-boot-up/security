package com.jk.explore.security.totpotp;

import javax.crypto.SecretKey;

/** Runs the online bank story in five numbered acts, with the java-otp library, and prints exact, repeatable results. */
public class OtpLibraryDemo {

    static final String USER = "dana";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1SameValues(out);
        act2WhatTheLibraryChecks(out);
        act3Options(out);
        act4Provisioning(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1SameValues(StringBuilder out) {
        line(out, "ONE. The library makes the same codes as the hand-written version.");
        OtpService otp = OtpService.standard(1, true);
        SecretKey key = Secrets.key(Secrets.SHA1, "HmacSHA1");
        StringBuilder codes = new StringBuilder();
        for (int step = 0; step < 4; step++) {
            codes.append(step == 0 ? "" : ", ").append(otp.codeAt(key, step * 30L));
        }
        line(out, "  codes for steps 0 to 3, at times 0, 30, 60 and 90: " + codes);
        line(out, "  at time 59 the code is " + otp.codeAt(key, 59) + "  <- THE VALUE IN RFC 4226 AND 6238");
    }

    private static void act2WhatTheLibraryChecks(StringBuilder out) {
        line(out, "TWO. What the library checks, and what it leaves to the bank.");
        OtpService otp = OtpService.standard(1, false);
        SecretKey key = Secrets.key(Secrets.SHA1, "HmacSHA1");
        String code = otp.codeAt(key, 59);
        line(out, "  the library says " + code + " is right at time 59, in step 1: " + otp.libraryValidates(key, 59, code));
        line(out, "  the library says " + code + " is right at time 40, also step 1: " + otp.libraryValidates(key, 40, code));
        line(out, "  the library says " + code + " is right at time 65, in step 2: " + otp.libraryValidates(key, 65, code)
                + "  <- ONE STEP ONLY");
        line(out, "  the bank's own window of 1 accepts it at time 65: " + otp.verify(USER, key, code, 65));
        line(out, "  and refuses it two steps on, at time 95, in step 3: " + otp.verify(USER, key, code, 95));
    }

    private static void act3Options(StringBuilder out) {
        line(out, "THREE. Other hashes and longer codes.");
        String[][] cases = {
            {"SHA-1", "HmacSHA1"}, {"SHA-256", "HmacSHA256"}, {"SHA-512", "HmacSHA512"}
        };
        byte[][] keys = {Secrets.SHA1, Secrets.SHA256, Secrets.SHA512};
        for (int i = 0; i < cases.length; i++) {
            OtpService otp = new OtpService(OtpService.generator(8, cases[i][1]), 1, true);
            line(out, "  " + cases[i][0] + ", 8 digits, at time 59: " + otp.codeAt(Secrets.key(keys[i], cases[i][1]), 59));
        }
        line(out, "  all three are the values of RFC 6238");
        line(out, "  some authenticator apps ignore these settings and use SHA-1, 6 digits, 30 seconds");
    }

    private static void act4Provisioning(StringBuilder out) {
        line(out, "FOUR. Giving the secret to the phone.");
        String uri = OtpAuthUri.totp("Example Bank", "dana@example.com", Secrets.SHA1, "SHA1", 6, 30);
        line(out, "  the QR code carries this text:");
        line(out, "  " + uri);
        line(out, "  the app reads it once and keeps the secret, which is why the screen must be private");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what the library does not do.");
        SecretKey key = Secrets.key(Secrets.SHA1, "HmacSHA1");
        OtpService open = OtpService.standard(1, false);
        String code = open.codeAt(key, 59);
        line(out, "  the library alone, the same code twice: " + open.verify(USER, key, code, 59) + ", "
                + open.verify(USER, key, code, 64) + "  <- NOT THE LIBRARY'S JOB");
        OtpService guarded = OtpService.standard(1, true);
        line(out, "  with the bank's own memory of the last step: " + guarded.verify(USER, key, code, 59) + ", "
                + guarded.verify(USER, key, code, 64));
        line(out, "  the library keeps no secrets, counts no wrong codes and limits no attempts");
        line(out, "  those, and the choice of window, are the bank's to write and to test");
    }
}
