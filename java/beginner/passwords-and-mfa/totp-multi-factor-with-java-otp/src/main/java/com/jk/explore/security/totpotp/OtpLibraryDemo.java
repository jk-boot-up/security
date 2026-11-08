package com.jk.explore.security.totpotp;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.SecretKey;

/**
 * Runs the online bank story in six numbered acts, with the java-otp library, and prints exact, repeatable results.
 * Each act prints its input and its result in separate, labelled sections.
 */
public class OtpLibraryDemo {

    static final String USER = "dana";
    private static final HexFormat HEX = HexFormat.of();

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1SameValues(out);
        act2HowTheCodeIsMade(out);
        act3WhatTheLibraryChecks(out);
        act4Options(out);
        act5Provisioning(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static SecretKey sha1Key() {
        return Secrets.key(Secrets.SHA1, "HmacSHA1");
    }

    private static void act1SameValues(StringBuilder out) {
        line(out, "ONE. The library's codes, checked against the standard.");
        OtpService otp = OtpService.standard(1, true);
        line(out, "  BEFORE: THE INPUTS");
        line(out, "    key       " + new String(Secrets.SHA1, StandardCharsets.US_ASCII) + "  (the RFC test key)");
        line(out, "    times     0, 30, 60, 90 and 59 seconds since 1970");
        line(out, "  AFTER: THE LIBRARY'S CODES");
        for (long time : new long[] {0, 30, 60, 90, 59}) {
            line(out, String.format("    time %-4d step %d   %s%s", time, time / 30, otp.codeAt(sha1Key(), time),
                    time == 59 ? "  <- THE VALUE IN RFC 6238" : ""));
        }
    }

    private static void act2HowTheCodeIsMade(StringBuilder out) {
        line(out, "TWO. How the code at time 59 is made, step by step.");
        TotpSteps.Steps s = TotpSteps.at(Secrets.SHA1, 59);
        line(out, "  STEP 1. THE TIME STEP = 59 / 30, AS 8 BYTES:");
        line(out, "    step      " + s.step());
        line(out, "    counter   " + spaced(HEX.formatHex(s.counter())));
        line(out, "  STEP 2. HMAC-SHA1(key, counter) = 20 BYTES:");
        line(out, "    hmac      " + spaced(HEX.formatHex(s.hmac())));
        line(out, "  STEP 3. TAKE 4 BYTES, FROM THE LAST HALF-BYTE ON:");
        line(out, "    last      " + HEX.toHexDigits(s.hmac()[19]) + ", so offset " + s.offset());
        line(out, "    4 bytes   " + String.format("%08x", s.fourBytes()));
        line(out, "  STEP 4. DROP THE TOP BIT, KEEP THE LAST 6 DIGITS:");
        line(out, "    number    " + s.number());
        line(out, "    code      " + s.code());
        line(out, "  the same code the library gave: "
                + s.code().equals(OtpService.standard(1, true).codeAt(sha1Key(), 59)));
    }

    private static void act3WhatTheLibraryChecks(StringBuilder out) {
        line(out, "THREE. What the library checks, and what the bank adds.");
        OtpService otp = OtpService.standard(1, false);
        String code = otp.codeAt(sha1Key(), 59);
        line(out, "  BEFORE: THE LIBRARY ALONE");
        line(out, "    " + code + " at time 59, step 1: " + otp.libraryValidates(sha1Key(), 59, code));
        line(out, "    " + code + " at time 40, step 1: " + otp.libraryValidates(sha1Key(), 40, code));
        line(out, "    " + code + " at time 65, step 2: " + otp.libraryValidates(sha1Key(), 65, code)
                + "  <- ONE STEP ONLY");
        line(out, "  AFTER: THE BANK'S WINDOW OF 1 STEP EITHER SIDE");
        line(out, "    " + code + " at time 65, step 2: " + otp.verify(USER, sha1Key(), code, 65));
        line(out, "    " + code + " at time 95, step 3: " + otp.verify(USER, sha1Key(), code, 95));
    }

    private static void act4Options(StringBuilder out) {
        line(out, "FOUR. Other hashes and longer codes.");
        String[][] cases = {{"SHA-1", "HmacSHA1"}, {"SHA-256", "HmacSHA256"}, {"SHA-512", "HmacSHA512"}};
        byte[][] keys = {Secrets.SHA1, Secrets.SHA256, Secrets.SHA512};
        line(out, "  BEFORE: THE SETTINGS, AT TIME 59");
        for (int i = 0; i < cases.length; i++) {
            line(out, String.format("    %-9s %d-byte key, 8 digits", cases[i][0], keys[i].length));
        }
        line(out, "  AFTER: THE CODES");
        for (int i = 0; i < cases.length; i++) {
            OtpService otp = new OtpService(OtpService.generator(8, cases[i][1]), 1, true);
            line(out, String.format("    %-9s %s", cases[i][0], otp.codeAt(Secrets.key(keys[i], cases[i][1]), 59)));
        }
        line(out, "  all three are the values of RFC 6238");
        line(out, "  many authenticator apps take only SHA-1, 6 digits, 30 seconds");
    }

    private static void act5Provisioning(StringBuilder out) {
        line(out, "FIVE. Giving the secret to the phone.");
        line(out, "  BEFORE: WHAT THE BANK PUTS IN");
        line(out, "    issuer    Example Bank");
        line(out, "    account   dana@example.com");
        line(out, "    secret    " + new String(Secrets.SHA1, StandardCharsets.US_ASCII) + "  (20 bytes)");
        line(out, "    settings  SHA1, 6 digits, 30 seconds");
        String uri = OtpAuthUri.totp("Example Bank", "dana@example.com", Secrets.SHA1, "SHA1", 6, 30);
        line(out, "  AFTER: THE TEXT IN THE QR CODE");
        // one link, printed one part per line so it fits the screen
        String[] parts = uri.replace("?", "\n?").replace("&issuer", "\n&issuer").replace("&algorithm", "\n&algorithm")
                .split("\n");
        line(out, "    " + parts[0]);
        for (int i = 1; i < parts.length; i++) {
            line(out, "      " + parts[i]);
        }
        line(out, "  Base32: 20 bytes = 160 bits = 32 letters of 5 bits each");
        line(out, "  the app keeps the secret: show it once, in private");
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: what the library does not do.");
        String code = OtpService.standard(1, false).codeAt(sha1Key(), 59);
        OtpService open = OtpService.standard(1, false);
        line(out, "  BEFORE THE FIX: THE LIBRARY ALONE");
        line(out, "    login at time 59 with " + code + ": " + open.verify(USER, sha1Key(), code, 59));
        line(out, "    login at time 64 with " + code + ": " + open.verify(USER, sha1Key(), code, 64)
                + "  <- USED TWICE");
        OtpService guarded = OtpService.standard(1, true);
        line(out, "  AFTER THE FIX: THE BANK REMEMBERS THE LAST STEP");
        line(out, "    login at time 59 with " + code + ": " + guarded.verify(USER, sha1Key(), code, 59));
        line(out, "    login at time 64 with " + code + ": " + guarded.verify(USER, sha1Key(), code, 64));
        line(out, "  still the bank's: secret storage, wrong-code counts,");
        line(out, "  attempt limits, and the choice of window");
    }

    /** "0000000000000001" -> "00000000 00000001": hex in groups of 4 bytes. */
    private static String spaced(String hex) {
        return String.join(" ", hex.split("(?<=\\G.{8})"));
    }
}
