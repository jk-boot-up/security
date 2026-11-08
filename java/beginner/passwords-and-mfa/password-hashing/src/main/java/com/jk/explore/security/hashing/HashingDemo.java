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

    /** A 32-byte hash over two lines of four groups, the second line under the first. */
    private static void hashLines(StringBuilder out, String label, String hex) {
        String grouped = Hex.grouped(hex);
        line(out, String.format("    %-12s%s", label, grouped.substring(0, 35)));
        line(out, String.format("    %-12s%s", "", grouped.substring(36)));
    }

    private static String half(byte[] bytes) {
        return Hex.grouped(bytes).substring(0, 35) + " ...";
    }

    private static void act1PlainText(StringBuilder out) {
        line(out, "ONE. Passwords stored as they are.");
        AccountStore store = fill(new PlainText(new WorkMeter()));
        line(out, "  BEFORE STORING (what ana typed):");
        line(out, "    password    " + PASSWORDS.get("ana"));
        line(out, "  AFTER STORING (ana's row in the table):");
        line(out, "    stored      " + store.storedFor("ana") + "  <- THE PASSWORD ITSELF");
        line(out, "  THE ATTACK (the table is stolen):");
        line(out, "    " + store.stolen().size() + " of " + PASSWORDS.size() + " passwords readable  <- NOTHING TO CRACK");
    }

    private static void act2Unsalted(StringBuilder out) {
        line(out, "TWO. A fast hash, with no salt.");
        WorkMeter meter = new WorkMeter();
        PasswordHasher hasher = new UnsaltedSha256(meter);
        AccountStore store = fill(hasher);
        line(out, "  BEFORE HASHING (what ana and ben typed):");
        line(out, "    ana         " + PASSWORDS.get("ana"));
        line(out, "    ben         " + PASSWORDS.get("ben"));
        line(out, "  AFTER HASHING with SHA-256 (32 bytes each):");
        hashLines(out, "ana", store.storedFor("ana"));
        line(out, "    ben         the same 32 bytes: " + store.storedFor("ana").equals(store.storedFor("ben"))
                + "  <- EASY TO SPOT");
        Cracked cracked = new Attacker(WORDLIST).crackWithTable(store.stolen(), hasher);
        line(out, "  THE ATTACK (a table made in advance):");
        line(out, "    " + WORDLIST.size() + " common passwords, each hashed once");
        line(out, "    cracked " + cracked.found().size() + " of " + PASSWORDS.size() + ": " + names(cracked.found()));
        line(out, "    one table fits every account  <- NO GUESSING PER ACCOUNT");
    }

    private static void act3Salted(StringBuilder out) {
        line(out, "THREE. A fast hash, with a salt for each account.");
        WorkMeter meter = new WorkMeter();
        PasswordHasher hasher = new SaltedSha256(meter, new CountingSalts());
        AccountStore store = fill(hasher);
        String[] ana = store.storedFor("ana").split("\\$");
        String[] ben = store.storedFor("ben").split("\\$");
        byte[] typed = PASSWORDS.get("ana").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        line(out, "  BEFORE HASHING (ana: her salt, then her password):");
        line(out, "    salt        " + Hex.grouped(ana[0]));
        line(out, "    password    " + PASSWORDS.get("ana") + " = " + Hex.grouped(typed));
        line(out, "    joined      salt + password = " + (ana[0].length() / 2 + typed.length) + " bytes");
        line(out, "  AFTER HASHING with SHA-256 (the joined bytes):");
        hashLines(out, "hash", ana[1]);
        line(out, "    stored      salt $ hash  (the salt is not a secret)");
        line(out, "  BEN: the same password, salt ending " + ben[0].substring(24));
        line(out, "    hash        " + Hex.grouped(ben[1]).substring(0, 35) + " ...");
        line(out, "    same stored value as ana: " + store.storedFor("ana").equals(store.storedFor("ben")));
        meter.reset();
        Cracked cracked = new Attacker(WORDLIST).crackEachAccount(store.stolen(), hasher);
        line(out, "  THE ATTACK (each account on its own):");
        line(out, "    the table made in advance fits no account");
        line(out, "    cracked " + cracked.found().size() + " of " + PASSWORDS.size() + " in " + cracked.guesses()
                + " guesses");
        line(out, "    " + meter.steps() + " steps of work  <- STILL CHEAP");
    }

    private static void act4Slow(StringBuilder out) {
        line(out, "FOUR. A slow hash, with a salt for each account.");
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), CURRENT_ROUNDS);
        AccountStore store = fill(hasher);
        String[] parts = store.storedFor("ana").split("\\$");
        line(out, "  BEFORE HASHING (ana):");
        line(out, "    password    " + PASSWORDS.get("ana"));
        line(out, "    salt        " + Hex.grouped(parts[2]));
        line(out, "    rounds      " + CURRENT_ROUNDS);
        line(out, "  AFTER HASHING with PBKDF2 (the stored text, four parts):");
        line(out, "    method      " + parts[0]);
        line(out, "    rounds      " + parts[1]);
        line(out, "    salt        " + Hex.grouped(parts[2]));
        hashLines(out, "hash", parts[3]);
        line(out, "    ana logs in with the right password: " + store.login("ana", "welcome1"));
        line(out, "    ana logs in with a wrong one: " + store.login("ana", "guess"));

        RoundSteps.Steps steps = RoundSteps.of(PASSWORDS.get("ana"),
                java.util.HexFormat.of().parseHex(parts[2]), CURRENT_ROUNDS);
        line(out, "  STEP 1. ROUND 1 = HMAC-SHA-256(password, salt + 00000001)");
        line(out, "    round 1      " + half(steps.firstRounds().get(0)));
        line(out, "  STEP 2. EACH NEXT ROUND = HMAC-SHA-256(password, round before)");
        line(out, "    round 2      " + half(steps.firstRounds().get(1)));
        line(out, "    round 3      " + half(steps.firstRounds().get(2)));
        line(out, "    ...          " + (CURRENT_ROUNDS - 4) + " more rounds");
        line(out, "    round " + CURRENT_ROUNDS + " " + half(steps.lastRound()));
        line(out, "  STEP 3. HASH = round 1 xor round 2 xor ... xor round " + CURRENT_ROUNDS);
        line(out, "    hash         " + half(steps.hash()));
        line(out, "    the same hash PBKDF2 gave: "
                + java.util.HexFormat.of().formatHex(steps.hash()).equals(parts[3]));

        meter.reset();
        Cracked cracked = new Attacker(WORDLIST).crackEachAccount(store.stolen(), hasher);
        line(out, "  THE ATTACK (the same " + cracked.guesses() + " guesses):");
        line(out, "    each guess costs " + hasher.stepsPerHash() + " steps, not 1");
        line(out, "    " + cracked.guesses() + " guesses: " + meter.steps() + " steps of work");
        line(out, "    cracked " + cracked.found().size() + " of " + PASSWORDS.size() + ": " + names(cracked.found()));
        line(out, "    <- SLOW HELPS, A WEAK PASSWORD STILL FALLS");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: login costs, and upgrades.");
        WorkMeter meter = new WorkMeter();
        Pbkdf2Hasher hasher = new Pbkdf2Hasher(meter, new CountingSalts(), CURRENT_ROUNDS);
        AccountStore store = new AccountStore(hasher);
        store.register("ana", "welcome1");
        store.restore("erin", hasher.hashWithRounds("letmein", LEGACY_ROUNDS));
        long perLogin = hasher.stepsPerHash();
        line(out, "  THE COST OF LOGINS:");
        line(out, "    one login         " + perLogin + " steps");
        line(out, "    1000 wrong logins " + (1000 * perLogin) + " steps  <- A TARGET");
        line(out, "  BEFORE THE UPGRADE (erin's account is years old):");
        line(out, "    erin        " + Pbkdf2Hasher.iterationsOf(store.storedFor("erin")) + " rounds");
        line(out, "    ana         " + Pbkdf2Hasher.iterationsOf(store.storedFor("ana")) + " rounds");
        line(out, "    erin logs in: " + store.login("erin", "letmein"));
        line(out, "  AFTER THE UPGRADE (a new hash, made at that login):");
        line(out, "    erin        " + Pbkdf2Hasher.iterationsOf(store.storedFor("erin")) + " rounds");
        line(out, "    erin logs in again: " + store.login("erin", "letmein"));
    }
}
