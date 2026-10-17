package com.jk.explore.security.totp;

import java.util.List;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
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
        act3Login(out);
        act4Replay(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1PasswordAlone(StringBuilder out) {
        line(out, "ONE. A password alone.");
        MfaServer bank = bank(1, true);
        line(out, "  an attacker has dana's password from another site's breach");
        line(out, "  the attacker logs in: " + bank.login(USER, PASSWORD, null, 59) + "  <- ONE FACTOR, ONE LEAK");
    }

    private static void act2Enrol(StringBuilder out) {
        line(out, "TWO. Enrolling a second factor.");
        MfaServer bank = bank(1, true);
        String secret = bank.enroll(USER);
        line(out, "  the bank makes a 20-byte secret and shows it as Base32: " + secret);
        byte[] key = Base32.decode(secret);
        line(out, "  at time 59 the counter is " + Totp.counterAt(59) + " and the code is " + Totp.codeAt(key, 59));
        StringBuilder codes = new StringBuilder();
        for (int step = 0; step < 4; step++) {
            codes.append(step == 0 ? "" : ", ").append(Hotp.code(key, step, Totp.DIGITS));
        }
        line(out, "  the code for steps 0 to 3, each lasting 30 seconds: " + codes);
        line(out, "  the phone and the bank each work it out from the secret and the time, and never talk");
    }

    private static void act3Login(StringBuilder out) {
        line(out, "THREE. Logging in with a code, and clocks that disagree.");
        MfaServer bank = bank(1, true);
        byte[] key = Base32.decode(bank.enroll(USER));
        Authenticator onTime = new Authenticator(key, 0);
        line(out, "  the password alone: " + bank.login(USER, PASSWORD, null, 59));
        line(out, "  dana's phone shows " + onTime.codeShownAt(59) + " at time 59: " + bank.login(USER, PASSWORD, onTime.codeShownAt(59), 59));
        MfaServer fresh = bank(1, true);
        byte[] key2 = Base32.decode(fresh.enroll(USER));
        line(out, "  a wrong code, 000000: " + fresh.login(USER, PASSWORD, "000000", 59));
        Authenticator fast25 = new Authenticator(key2, 25);
        line(out, "  a phone 25 seconds fast shows " + fast25.codeShownAt(59) + ": " + fresh.login(USER, PASSWORD, fast25.codeShownAt(59), 59)
                + " (the bank allows 1 step either side)");
        MfaServer third = bank(1, true);
        byte[] key3 = Base32.decode(third.enroll(USER));
        Authenticator fast70 = new Authenticator(key3, 70);
        line(out, "  a phone 70 seconds fast shows " + fast70.codeShownAt(59) + ": " + third.login(USER, PASSWORD, fast70.codeShownAt(59), 59)
                + " (3 steps ahead)");
        MfaServer fourth = bank(1, true);
        byte[] key4 = Base32.decode(fourth.enroll(USER));
        String old = Hotp.code(key4, 2, Totp.DIGITS);
        line(out, "  an old code, from step 2, used at time 120 (step 4): " + fourth.login(USER, PASSWORD, old, 120));
    }

    private static void act4Replay(StringBuilder out) {
        line(out, "FOUR. The same code twice.");
        for (boolean protect : new boolean[] {false, true}) {
            MfaServer bank = bank(1, protect);
            byte[] key = Base32.decode(bank.enroll(USER));
            String code = new Authenticator(key, 0).codeShownAt(59);
            Verdict dana = bank.login(USER, PASSWORD, code, 59);
            Verdict attacker = bank.login(USER, PASSWORD, code, 64);
            line(out, "  " + (protect ? "with" : "without") + " replay protection: dana logs in with " + code + ": " + dana
                    + "; an observer uses it 5 seconds later: " + attacker + (protect ? "" : "  <- REPLAY"));
        }
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what a code cannot do.");
        MfaServer bank = bank(1, true);
        byte[] key = Base32.decode(bank.enroll(USER));
        Authenticator phone = new Authenticator(key, 0);
        String typedOnFakeSite = phone.codeShownAt(59);
        line(out, "  dana types her password and code " + typedOnFakeSite + " into a fake bank page, which passes both on at once: "
                + bank.login(USER, PASSWORD, typedOnFakeSite, 61) + "  <- CANNOT TELL THE FAKE PAGE FROM THE REAL ONE");
        byte[] stolen = bank.stolenSecrets().get(USER);
        line(out, "  the bank's table is stolen, and it holds the secret: the attacker's own app shows "
                + Totp.codeAt(stolen, 59) + " at time 59, the same as dana's  <- THE SECRET LIVES ON THE SERVER TOO");
        List<String> recovery = bank.issueRecoveryCodes(USER, 3);
        line(out, "  dana loses her phone and uses a recovery code: " + bank.loginWithRecoveryCode(USER, PASSWORD, recovery.get(0)));
        line(out, "  the same recovery code again: " + bank.loginWithRecoveryCode(USER, PASSWORD, recovery.get(0)));
        line(out, "  a recovery code is a second way in, so it must be guarded like the password");
    }
}
