package com.jk.explore.security.attacks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Each act prints what goes in
 * (THE REQUEST, THE DICTIONARY, THE SPRAY, THE LEAK) apart from what comes out (THE RESPONSE, THE RESULT).
 */
public class AttacksDemo {

    static final List<String> WORDLIST = List.of(
            "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234", "111111", "1234567",
            "dragon", "123123", "baseball", "abc123", "midnight7", "letmein", "monkey", "welcome1", "sunshine",
            "iloveyou", "admin");

    static final List<String> SPRAY_PASSWORDS = List.of("welcome1", "hunter2", "123456");

    static final int LOCK_AFTER = 5;
    static final int LOCK_TICKS = 10;
    static final int LIMIT_PER_WINDOW = 5;
    static final int WINDOW_TICKS = 10;

    /** username, password, uses a second factor */
    static final String[][] CUSTOMERS = {
        {"dana", "midnight7", "no"}, {"emil", "welcome1", "no"}, {"farah", "welcome1", "no"},
        {"gus", "T7#xqw!9Lp", "no"}, {"hana", "welcome1", "no"}, {"ivan", "hunter2", "no"},
        {"joel", "sunrise88", "no"}, {"kate", "Kx3!mq7#Zp", "no"}, {"liam", "football5", "no"}, {"mona", "R4v!t9#Lq2", "no"}
    };

    /** Pairs leaked from another site: four of them are the same password the customer uses at the bank. */
    static final Map<String, String> LEAKED = new LinkedHashMap<>();

    static {
        LEAKED.put("dana", "midnight7");
        LEAKED.put("emil", "emil-other-site");
        LEAKED.put("farah", "welcome1");
        LEAKED.put("gus", "gus-other-site");
        LEAKED.put("hana", "hana-other-site");
        LEAKED.put("ivan", "ivan-other-site");
        LEAKED.put("joel", "sunrise88");
        LEAKED.put("kate", "kate-other-site");
        LEAKED.put("liam", "football5");
        LEAKED.put("mona", "mona-other-site");
    }

    static Bank bank(Ticker ticker, Defences defences, Set<String> withSecondFactor) {
        Bank bank = new Bank(ticker, defences);
        for (String[] c : CUSTOMERS) {
            bank.open(c[0], c[1], withSecondFactor.contains(c[0]));
        }
        return bank;
    }

    static List<String> usernames() {
        return java.util.Arrays.stream(CUSTOMERS).map(c -> c[0]).toList();
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1NoDefences(out);
        act2Lockout(out);
        act3RateLimit(out);
        act4Stuffing(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String shortHash(String hex) {
        return hex.substring(0, 16) + "...";
    }

    private static void act1NoDefences(StringBuilder out) {
        line(out, "ONE. No defences.");
        Bank bank = bank(new Ticker(), Defences.none(), Set.of());
        line(out, "  THE REQUEST: one guess at dana's password");
        line(out, "    from attacker   name dana   password 123456");
        line(out, "  THE CHECK: SHA-256 of \"demo-salt:dana:\" + password");
        line(out, "    guess hash    " + shortHash(Bank.hashOf("dana", "123456")) + "  (123456)");
        line(out, "    stored hash   " + shortHash(Bank.hashOf("dana", "midnight7")) + "  (midnight7)");
        line(out, "    the same?     " + Bank.hashOf("dana", "123456").equals(Bank.hashOf("dana", "midnight7")));
        Reply reply = bank.login("attacker", "dana", "123456");
        line(out, "  THE RESPONSE:");
        line(out, "    " + reply.outcome() + "  \"" + reply.message() + "\"");

        Ticker t1 = new Ticker();
        Attacks dictionary = new Attacks(bank(t1, Defences.none(), Set.of()), t1);
        dictionary.dictionary("attacker", "dana", WORDLIST);
        line(out, "  THE DICTIONARY: " + WORDLIST.size() + " common words on dana, 1 per tick");
        timeline(out, dictionary.steps());
        line(out, "  THE RESULT: found at tick 13  <- NOTHING STOPS IT");

        Ticker t2 = new Ticker();
        Attacks spray = new Attacks(bank(t2, Defences.none(), Set.of()), t2);
        Found sprayed = spray.spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  THE SPRAY: " + SPRAY_PASSWORDS.size() + " common passwords on all " + usernames().size() + " accounts");
        line(out, "    accounts  " + String.join(" ", usernames()));
        for (String password : SPRAY_PASSWORDS) {
            List<Step> tries = spray.steps().stream().filter(s -> s.password().equals(password)).toList();
            List<String> taken = tries.stream().filter(s -> s.outcome() == Outcome.OK).map(Step::username).toList();
            line(out, String.format("    %-9s %2d tries   taken: %s", password, tries.size(),
                    taken.isEmpty() ? "nobody" : String.join(", ", taken)));
        }
        line(out, "  THE RESULT: " + sprayed.taken().size() + " of " + usernames().size() + " taken in "
                + sprayed.guesses() + " guesses, at tick " + sprayed.ticks());
        line(out, "    most wrong guesses on one account: " + mostFailures(spray.steps()));
    }

    private static void timeline(StringBuilder out, List<Step> steps) {
        for (String run : Timeline.runs(steps)) {
            line(out, "    " + run);
        }
    }

    private static long mostFailures(List<Step> steps) {
        return steps.stream().filter(s -> s.outcome() == Outcome.WRONG_PASSWORD)
                .collect(java.util.stream.Collectors.groupingBy(Step::username, java.util.stream.Collectors.counting()))
                .values().stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private static void act2Lockout(StringBuilder out) {
        line(out, "TWO. Lock an account after " + LOCK_AFTER + " wrong passwords, for " + LOCK_TICKS + " ticks.");
        Defences lock = Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT);
        Ticker t1 = new Ticker();
        Attacks dictionary = new Attacks(bank(t1, lock, Set.of()), t1);
        Found found = dictionary.dictionary("attacker", "dana", WORDLIST);
        line(out, "  THE DICTIONARY: the same " + WORDLIST.size() + " words on dana");
        timeline(out, dictionary.steps());
        line(out, "  THE RESULT: found at tick " + found.taken().get("dana") + " instead of 13  <- SLOWED");
        Ticker t2 = new Ticker();
        Attacks spray = new Attacks(bank(t2, lock, Set.of()), t2);
        Found sprayed = spray.spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  THE SPRAY: the same " + SPRAY_PASSWORDS.size() + " passwords on all " + usernames().size() + " accounts");
        line(out, "    most wrong guesses on one account: " + mostFailures(spray.steps())
                + ", the lock needs " + LOCK_AFTER);
        line(out, "  THE RESULT: " + sprayed.taken().size() + " taken by tick " + sprayed.ticks()
                + "  <- NOT SLOWED AT ALL");
    }

    private static void act3RateLimit(StringBuilder out) {
        line(out, "THREE. Allow each address " + LIMIT_PER_WINDOW + " attempts in every " + WINDOW_TICKS + " ticks.");
        Defences limit = Defences.none().withRateLimit(LIMIT_PER_WINDOW, WINDOW_TICKS);
        Ticker t1 = new Ticker();
        Attacks dictionary = new Attacks(bank(t1, limit, Set.of()), t1);
        Found found = dictionary.dictionary("attacker", "dana", WORDLIST);
        line(out, "  THE DICTIONARY: the same " + WORDLIST.size() + " words on dana, from 1 address");
        timeline(out, dictionary.steps());
        line(out, "  THE RESULT: found at tick " + found.taken().get("dana") + " instead of 13");
        Ticker t2 = new Ticker();
        Found sprayed = new Attacks(bank(t2, limit, Set.of()), t2).spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  THE SPRAY: the same " + SPRAY_PASSWORDS.size() + " passwords on all " + usernames().size() + " accounts");
        line(out, "  THE RESULT: " + sprayed.taken().size() + " taken by tick " + sprayed.ticks()
                + ", not 23  <- SLOWED, NOT STOPPED");
    }

    private static String who(List<Step> steps, Outcome outcome) {
        List<String> names = steps.stream().filter(s -> s.outcome() == outcome).map(Step::username).toList();
        return names.isEmpty() ? "nobody" : String.join(", ", names);
    }

    private static void act4Stuffing(StringBuilder out) {
        line(out, "FOUR. Credential stuffing: passwords leaked from another site.");
        line(out, "  THE LEAK: " + LEAKED.size() + " name and password pairs from another site");
        line(out, "    each pair tried once, each from its own address");
        Defences both = Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT)
                .withRateLimit(LIMIT_PER_WINDOW, WINDOW_TICKS);
        Ticker t1 = new Ticker();
        Attacks plain = new Attacks(bank(t1, both, Set.of()), t1);
        Map<Outcome, Integer> counts = plain.stuff(LEAKED);
        line(out, "  THE RESULT, with a lockout and a rate limit:");
        line(out, "    " + counts.getOrDefault(Outcome.OK, 0) + " of " + LEAKED.size() + " taken: "
                + who(plain.steps(), Outcome.OK) + "  <- NOTHING TRIGGERS");
        Ticker t2 = new Ticker();
        Attacks mfa = new Attacks(bank(t2, both, Set.of("dana", "joel")), t2);
        mfa.stuff(LEAKED);
        line(out, "  THE RESULT, with a second factor on dana and joel:");
        line(out, "    taken: " + who(mfa.steps(), Outcome.OK));
        line(out, "    stopped at the second factor: " + who(mfa.steps(), Outcome.SECOND_FACTOR_NEEDED));
        Ticker t3 = new Ticker();
        Defences checked = both.withBreachedCheck(Set.copyOf(LEAKED.values()));
        Attacks breached = new Attacks(bank(t3, checked, Set.of()), t3);
        breached.stuff(LEAKED);
        line(out, "  THE RESULT, with a check against known breaches:");
        line(out, "    taken: " + who(breached.steps(), Outcome.OK));
        line(out, "    must choose a new password: " + who(breached.steps(), Outcome.RESET_REQUIRED));
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what the defences cost.");
        line(out, "  THE ATTACK: " + LOCK_AFTER + " wrong passwords for dana, from attacker");
        Ticker t1 = new Ticker();
        Bank byAccount = bank(t1, Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT), Set.of());
        for (int i = 0; i < LOCK_AFTER; i++) {
            byAccount.login("attacker", "dana", "wrong" + i);
            t1.advance(1);
        }
        Ticker t2 = new Ticker();
        Bank byPair = bank(t2, Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT_AND_ADDRESS), Set.of());
        for (int i = 0; i < LOCK_AFTER; i++) {
            byPair.login("attacker", "dana", "wrong" + i);
            t2.advance(1);
        }
        line(out, "  THE REQUEST: dana, the right password, from dana-phone");
        line(out, "  THE RESPONSE:");
        line(out, "    lock per account:  " + byAccount.login("dana-phone", "dana", "midnight7").outcome()
                + "  <- DENIAL OF SERVICE");
        line(out, "    lock per pair:     " + byPair.login("dana-phone", "dana", "midnight7").outcome());
        line(out, "    but 10 addresses then get 10 x " + LOCK_AFTER + " = " + (10 * LOCK_AFTER)
                + " guesses, not " + LOCK_AFTER);

        Bank open = bank(new Ticker(), Defences.none(), Set.of());
        Bank generic = bank(new Ticker(), Defences.none().withGenericMessages(), Set.of());
        line(out, "  THE REQUEST: zed (no account), emil (wrong password)");
        line(out, "  THE RESPONSE, with a message for each case:");
        line(out, "    zed:   \"" + open.login("probe", "zed", "x").message() + "\"");
        line(out, "    emil:  \"" + open.login("probe", "emil", "x").message() + "\"  <- LEAKS WHICH NAMES EXIST");
        line(out, "  THE RESPONSE, with one message for both:");
        line(out, "    zed:   \"" + generic.login("probe", "zed", "x").message() + "\"");
        line(out, "    emil:  \"" + generic.login("probe", "emil", "x").message() + "\"");
        int before = generic.hashesComputed();
        generic.login("probe", "zed", "y");
        int forUnknown = generic.hashesComputed() - before;
        generic.login("probe", "emil", "y");
        int forKnown = generic.hashesComputed() - before - forUnknown;
        line(out, "    work behind it: " + forUnknown + " password hash for zed, " + forKnown + " for emil");
    }
}
