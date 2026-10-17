package com.jk.explore.security.hashing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class HashingDemo {

    static final Map<String, String> PASSWORDS = new java.util.LinkedHashMap<>();

    static {
        PASSWORDS.put("ana", "welcome1");
        PASSWORDS.put("ben", "welcome1");
        PASSWORDS.put("carol", "sunshine");
        PASSWORDS.put("dave", "k9#Tz!vQ2m");
        PASSWORDS.put("erin", "letmein");
    }

    static final List<String> WORDLIST = List.of(
            "123456", "password", "welcome1", "sunshine", "qwerty", "letmein", "iloveyou", "admin");

    static final int LEGACY_ROUNDS = 10_000;
    static final int CURRENT_ROUNDS = 100_000;

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1PlainText(out);
        act2Unsalted(out);
        act3Salted(out);
        act4Slow(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static AccountStore fill(PasswordHasher hasher) {
        AccountStore store = new AccountStore(hasher);
        PASSWORDS.forEach(store::register);
        return store;
    }

    private static String names(Map<String, String> found) {
        return found.isEmpty() ? "nobody" : found.keySet().stream().collect(Collectors.joining(", "));
    }

    private static String shortHash(String stored) {
        String text = stored.contains("$") ? stored.substring(stored.lastIndexOf('$') + 1) : stored;
        return text.substring(0, Math.min(8, text.length()));
    }

    private static void act1PlainText(StringBuilder out) {
        line(out, "ONE. Passwords stored as they are.");
        AccountStore store = fill(new PlainText(new WorkMeter()));
        line(out, "  the database is stolen: " + store.stolen().size() + " of " + PASSWORDS.size() + " passwords readable");
        line(out, "  ana's row: " + store.storedFor("ana") + "  <- NOTHING TO CRACK");
    }

    private static void act2Unsalted(StringBuilder out) {
        line(out, "TWO. A fast hash, with no salt.");
        WorkMeter meter = new WorkMeter();
        PasswordHasher hasher = new UnsaltedSha256(meter);
        AccountStore store = fill(hasher);
        line(out, "  ana's stored value begins " + shortHash(store.storedFor("ana"))
                + ", ben's begins " + shortHash(store.storedFor("ben")));
        line(out, "  same password, same stored value: " + store.storedFor("ana").equals(store.storedFor("ben"))
                + "  <- EASY TO SPOT");
        Cracked cracked = new Attacker(WORDLIST).crackWithTable(store.stolen(), hasher);
        line(out, "  a table of " + WORDLIST.size() + " common passwords is hashed once and looked up");
        line(out, "  cracked " + cracked.found().size() + " of " + PASSWORDS.size() + ": " + names(cracked.found())
                + "  <- NO GUESSING PER ACCOUNT");
    }

    private static void act3Salted(StringBuilder out) {
        line(out, "THREE. A fast hash, with a salt for each account.");
        WorkMeter meter = new WorkMeter();
        PasswordHasher hasher = new SaltedSha256(meter, new CountingSalts());
        AccountStore store = fill(hasher);
        line(out, "  ana's stored value begins " + shortHash(store.storedFor("ana"))
                + ", ben's begins " + shortHash(store.storedFor("ben")));
        line(out, "  same password, same stored value: " + store.storedFor("ana").equals(store.storedFor("ben")));
        meter.reset();
        Cracked cracked = new Attacker(WORDLIST).crackEachAccount(store.stolen(), hasher);
        line(out, "  the table trick is gone; each account needs its own guesses");
        line(out, "  cracked " + cracked.found().size() + " of " + PASSWORDS.size() + " in " + cracked.guesses()
                + " guesses, " + meter.steps() + " steps of work  <- STILL CHEAP");
    }

    private static void act4Slow(StringBuilder out) {
        line(out, "FOUR. A slow hash, with a salt for each account.");
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), CURRENT_ROUNDS);
        AccountStore store = fill(hasher);
        String[] parts = store.storedFor("ana").split("\\$");
        line(out, "  ana's stored text has four parts: " + parts[0] + ", " + parts[1] + " rounds, a salt beginning "
                + parts[2].substring(0, 8) + ", and a hash beginning " + parts[3].substring(0, 8));
        line(out, "  ana logs in with the right password: " + store.login("ana", "welcome1"));
        line(out, "  ana logs in with a wrong one: " + store.login("ana", "guess"));
        meter.reset();
        Cracked cracked = new Attacker(WORDLIST).crackEachAccount(store.stolen(), hasher);
        line(out, "  the same attack, the same " + cracked.guesses() + " guesses: " + meter.steps() + " steps of work");
        line(out, "  each guess costs " + hasher.stepsPerHash() + " times more than a fast hash");
        line(out, "  cracked " + cracked.found().size() + " of " + PASSWORDS.size() + ": " + names(cracked.found())
                + "  <- SLOW HELPS, A WEAK PASSWORD STILL FALLS");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: every login pays, and old hashes must be upgraded.");
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), CURRENT_ROUNDS);
        AccountStore store = new AccountStore(hasher);
        store.register("ana", "welcome1");
        store.restore("erin", hasher.hashWithRounds("letmein", LEGACY_ROUNDS));
        long perLogin = hasher.stepsPerHash();
        line(out, "  one login costs " + perLogin + " steps of work");
        line(out, "  1000 wrong logins cost " + (1000 * perLogin) + " steps  <- LOGIN CAN BE A TARGET");
        line(out, "  erin's stored hash uses " + Pbkdf2Hasher.iterationsOf(store.storedFor("erin")) + " rounds, ana's uses "
                + Pbkdf2Hasher.iterationsOf(store.storedFor("ana")));
        line(out, "  erin logs in: " + store.login("erin", "letmein") + ", now "
                + Pbkdf2Hasher.iterationsOf(store.storedFor("erin")) + " rounds");
        line(out, "  erin logs in again: " + store.login("erin", "letmein"));
    }
}
