package com.jk.explore.security.totp;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/** Runs the online bank story in six numbered acts and prints exact, repeatable results. */
public class TotpDemo {

    static final String USER = "dana";
    static final String PASSWORD = "midnight7";

    static MfaServer bank(int window, boolean replayProtection) {
        return new MfaServer(new DemoSecrets(), window, replayProtection).register(USER, PASSWORD);
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1PasswordAlone(out);
        act2Enrol(out);
        act3CodeSteps(out);
        act4Login(out);
        act5Replay(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A label padded to one column, then its value: "    code        287082". */
    private static void row(StringBuilder out, String label, Object value) {
        line(out, String.format("    %-12s%s", label, value));
    }

    private static String hex(byte[] bytes, int group) {
        String h = HexFormat.of().formatHex(bytes);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < h.length(); i += group * 2) {
            s.append(i == 0 ? "" : " ").append(h, i, Math.min(h.length(), i + group * 2));
        }
        return s.toString();
    }

    /** A login on a new bank with the code given, so the attempts do not affect each other. */
    private static Verdict freshLogin(String code, long now) {
        MfaServer bank = bank(1, true);
        bank.enroll(USER);
        return bank.login(USER, PASSWORD, code, now);
    }

    private static void act1PasswordAlone(StringBuilder out) {
        line(out, "ONE. A password alone.");
        MfaServer bank = bank(1, true);
        line(out, "  THE LOGIN");
        row(out, "user", USER);
        row(out, "password", PASSWORD + ", leaked from another site");
        row(out, "code", "none: the bank asks only for the password");
        line(out, "  THE ANSWER");
        row(out, "attacker", bank.login(USER, PASSWORD, null, 59) + "  <- ONE FACTOR, ONE LEAK");
    }

    private static void act2Enrol(StringBuilder out) {
        line(out, "TWO. Enrolling: one secret, shared once.");
        MfaServer bank = bank(1, true);
        String secret = bank.enroll(USER);
        byte[] key = Base32.decode(secret);
        line(out, "  BEFORE ENCODING: THE SECRET, 20 BYTES");
        row(out, "text", new String(key, StandardCharsets.US_ASCII));
        line(out, "    hex");
        line(out, "      " + hex(key, 4));
        line(out, "  AFTER ENCODING: BASE32 TEXT, FOR THE APP");
        row(out, "Base32", secret.replaceAll("(.{8})(?!$)", "$1 "));
        line(out, "    each 5 bytes become 8 letters, so 20 bytes give 32");
        line(out, "  THE CODES IT GIVES, ONE PER 30-SECOND STEP");
        for (int step = 0; step < 4; step++) {
            row(out, "step " + step, String.format("time %3d to %3d   %s", step * 30, step * 30 + 29,
                    Hotp.code(key, step, Totp.DIGITS)));
        }
    }

    private static void act3CodeSteps(StringBuilder out) {
        line(out, "THREE. How the code is made, step by step.");
        byte[] key = Base32.decode(bank(1, true).enroll(USER));
        CodeSteps.Steps s = CodeSteps.at(key, 59);
        line(out, "  BEFORE: THE SECRET AND THE TIME");
        row(out, "secret", new String(key, StandardCharsets.US_ASCII));
        row(out, "time", "59 seconds");
        line(out, "  STEP 1. COUNTER = TIME / 30, AS 8 BYTES");
        row(out, "counter", s.counter() + " = " + hex(s.counterBytes(), 4));
        line(out, "  STEP 2. HMAC-SHA1 OF THE COUNTER, WITH THE SECRET");
        line(out, "    hmac, 20 bytes");
        line(out, "      " + hex(s.hmac(), 4));
        line(out, "  STEP 3. THE LAST HALF-BYTE PICKS WHERE TO READ 4 BYTES");
        row(out, "last byte", String.format("%02x, last half-byte %x = %d", s.hmac()[19], s.offset(), s.offset()));
        row(out, "bytes " + s.offset() + "-" + (s.offset() + 3), hex(s.picked(), 4));
        line(out, "  STEP 4. DROP THE TOP BIT, KEEP THE LAST 6 DIGITS");
        row(out, "number", s.number());
        row(out, "last 6", s.code());
        line(out, "  AFTER: THE CODE");
        row(out, "code", s.code() + ", the same as Totp gives: " + s.code().equals(Totp.codeAt(key, 59)));
    }

    private static void act4Login(StringBuilder out) {
        line(out, "FOUR. Logging in, and clocks that disagree.");
        byte[] key = Base32.decode(bank(1, true).enroll(USER));
        String onTime = new Authenticator(key, 0).codeShownAt(59);
        String fast25 = new Authenticator(key, 25).codeShownAt(59);
        String fast70 = new Authenticator(key, 70).codeShownAt(59);
        line(out, "  THE ATTEMPTS, AT TIME 59 (STEP 1)");
        row(out, "a", "no code     the password alone");
        row(out, "b", onTime + "      phone on time (step 1)");
        row(out, "c", "000000      a wrong code");
        row(out, "d", fast25 + "      phone 25 seconds fast (step 2)");
        row(out, "e", fast70 + "      phone 70 seconds fast (step 4)");
        line(out, "  THE ANSWERS: THE BANK ALLOWS STEPS 0 TO 2");
        row(out, "a", freshLogin(null, 59));
        row(out, "b", freshLogin(onTime, 59));
        row(out, "c", freshLogin("000000", 59));
        row(out, "d", freshLogin(fast25, 59));
        row(out, "e", freshLogin(fast70, 59) + "  (3 steps ahead)");
    }

    private static void act5Replay(StringBuilder out) {
        line(out, "FIVE. The same code twice.");
        byte[] key = Base32.decode(bank(1, true).enroll(USER));
        String code = new Authenticator(key, 0).codeShownAt(59);
        line(out, "  THE LOGINS");
        row(out, "dana", code + " at time 59");
        row(out, "observer", code + " at time 64, 5 seconds later");
        for (boolean protect : new boolean[] {false, true}) {
            MfaServer bank = bank(1, protect);
            bank.enroll(USER);
            Verdict dana = bank.login(USER, PASSWORD, code, 59);
            Verdict observer = bank.login(USER, PASSWORD, code, 64);
            line(out, protect ? "  WITH REPLAY PROTECTION: THE BANK REMEMBERS STEP 1" : "  WITHOUT REPLAY PROTECTION");
            row(out, "dana", dana);
            row(out, "observer", observer + (protect ? "" : "  <- REPLAY"));
        }
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: what a code cannot do.");
        MfaServer bank = bank(1, true);
        String secret = bank.enroll(USER);
        String code = new Authenticator(Base32.decode(secret), 0).codeShownAt(59);
        line(out, "  A FAKE BANK PAGE: WHAT DANA TYPES INTO IT");
        row(out, "password", PASSWORD);
        row(out, "code", code);
        line(out, "  THE REAL BANK, WHEN THE PAGE PASSES THEM ON AT TIME 61");
        row(out, "login", bank.login(USER, PASSWORD, code, 61) + "  <- IT CANNOT TELL THE FAKE PAGE");
        byte[] stolen = bank.stolenSecrets().get(USER);
        line(out, "  A STOLEN SECRETS TABLE: WHAT IT HOLDS FOR DANA");
        row(out, "secret", Base32.encode(stolen));
        line(out, "  THE ATTACKER'S OWN APP, AT TIME 59");
        row(out, "code", Totp.codeAt(stolen, 59) + "  <- THE SAME AS DANA'S");
        List<String> recovery = bank.issueRecoveryCodes(USER, 3);
        line(out, "  A LOST PHONE: DANA TYPES A RECOVERY CODE");
        row(out, "first", recovery.get(0));
        row(out, "second", recovery.get(0) + ", again");
        line(out, "  THE BANK'S ANSWERS");
        row(out, "first", bank.loginWithRecoveryCode(USER, PASSWORD, recovery.get(0)));
        row(out, "second", bank.loginWithRecoveryCode(USER, PASSWORD, recovery.get(0)));
        line(out, "    a second way in: guard it like the password");
    }
}
