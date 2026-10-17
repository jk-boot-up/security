package com.jk.explore.security.attacks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
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

    private static String names(Map<String, Integer> taken) {
        return taken.isEmpty() ? "nobody" : String.join(", ", taken.keySet());
    }

    private static void act1NoDefences(StringBuilder out) {
        line(out, "ONE. No defences.");
        Ticker t1 = new Ticker();
        Found dictionary = new Attacks(bank(t1, Defences.none(), Set.of()), t1).dictionary("attacker", "dana", WORDLIST);
        line(out, "  dictionary attack on dana: found after " + dictionary.guesses() + " guesses, at tick "
                + dictionary.taken().get("dana") + "  <- NOTHING STOPS IT");
        Ticker t2 = new Ticker();
        Found spray = new Attacks(bank(t2, Defences.none(), Set.of()), t2).spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  spraying 3 common passwords over " + usernames().size() + " accounts: " + spray.taken().size()
                + " taken (" + names(spray.taken()) + ")");
        line(out, "  " + spray.guesses() + " guesses in all, finished at tick " + spray.ticks());
    }

    private static void act2Lockout(StringBuilder out) {
        line(out, "TWO. Lock an account after " + LOCK_AFTER + " wrong passwords, for " + LOCK_TICKS + " ticks.");
        Defences lock = Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT);
        Ticker t1 = new Ticker();
        Found dictionary = new Attacks(bank(t1, lock, Set.of()), t1).dictionary("attacker", "dana", WORDLIST);
        line(out, "  dictionary attack on dana: found at tick " + dictionary.taken().get("dana") + " instead of tick 13");
        Ticker t2 = new Ticker();
        Found spray = new Attacks(bank(t2, lock, Set.of()), t2).spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  the same spray: " + spray.taken().size() + " taken, finished at tick " + spray.ticks()
                + "  <- NO ACCOUNT FAILED 5 TIMES");
    }

    private static void act3RateLimit(StringBuilder out) {
        line(out, "THREE. Allow each address " + LIMIT_PER_WINDOW + " attempts in every " + WINDOW_TICKS + " ticks.");
        Defences limit = Defences.none().withRateLimit(LIMIT_PER_WINDOW, WINDOW_TICKS);
        Ticker t1 = new Ticker();
        Found dictionary = new Attacks(bank(t1, limit, Set.of()), t1).dictionary("attacker", "dana", WORDLIST);
        line(out, "  dictionary attack on dana: found at tick " + dictionary.taken().get("dana"));
        Ticker t2 = new Ticker();
        Found spray = new Attacks(bank(t2, limit, Set.of()), t2).spray("attacker", usernames(), SPRAY_PASSWORDS);
        line(out, "  the same spray: " + spray.taken().size() + " taken, but it needs " + spray.ticks()
                + " ticks instead of 23  <- SLOWED, NOT STOPPED");
    }

    private static void act4Stuffing(StringBuilder out) {
        line(out, "FOUR. Credential stuffing: passwords leaked from another site.");
        line(out, "  " + LEAKED.size() + " leaked pairs, each tried once, each from a different address");
        Defences both = Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT)
                .withRateLimit(LIMIT_PER_WINDOW, WINDOW_TICKS);
        Ticker t1 = new Ticker();
        Map<Outcome, Integer> plain = new Attacks(bank(t1, both, Set.of()), t1).stuff(LEAKED);
        line(out, "  with a lockout and a rate limit: " + plain.getOrDefault(Outcome.OK, 0) + " of " + LEAKED.size()
                + " accounts taken  <- NEITHER EVER TRIGGERS");
        Ticker t2 = new Ticker();
        Map<Outcome, Integer> mfa = new Attacks(bank(t2, both, Set.of("dana", "joel")), t2).stuff(LEAKED);
        line(out, "  with a second factor on dana and joel: " + mfa.getOrDefault(Outcome.OK, 0) + " taken, "
                + mfa.getOrDefault(Outcome.SECOND_FACTOR_NEEDED, 0) + " stopped at the second factor");
        Ticker t3 = new Ticker();
        Defences checked = both.withBreachedCheck(Set.copyOf(LEAKED.values()));
        Map<Outcome, Integer> breached = new Attacks(bank(t3, checked, Set.of()), t3).stuff(LEAKED);
        line(out, "  with a check against known breaches: " + breached.getOrDefault(Outcome.OK, 0) + " taken, "
                + breached.getOrDefault(Outcome.RESET_REQUIRED, 0) + " customers forced to choose a new password");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a lock can be used against the customer, and messages can leak.");
        Ticker t1 = new Ticker();
        Bank byAccount = bank(t1, Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT), Set.of());
        for (int i = 0; i < LOCK_AFTER; i++) {
            byAccount.login("attacker", "dana", "wrong" + i);
            t1.advance(1);
        }
        line(out, "  an attacker fails " + LOCK_AFTER + " times on dana's account, then dana logs in with the right password: "
                + byAccount.login("dana-phone", "dana", "midnight7").outcome() + "  <- DENIAL OF SERVICE");
        Ticker t2 = new Ticker();
        Bank byPair = bank(t2, Defences.none().withLockout(LOCK_AFTER, LOCK_TICKS, LockScope.ACCOUNT_AND_ADDRESS), Set.of());
        for (int i = 0; i < LOCK_AFTER; i++) {
            byPair.login("attacker", "dana", "wrong" + i);
            t2.advance(1);
        }
        line(out, "  locking each account-and-address pair instead: dana logs in: "
                + byPair.login("dana-phone", "dana", "midnight7").outcome());
        line(out, "  but 10 addresses get 10 x " + LOCK_AFTER + " = " + (10 * LOCK_AFTER)
                + " guesses before any of them is locked");

        Bank open = bank(new Ticker(), Defences.none(), Set.of());
        line(out, "  unknown name zed: \"" + open.login("probe", "zed", "x").message() + "\"");
        line(out, "  known name emil, wrong password: \"" + open.login("probe", "emil", "x").message()
                + "\"  <- LEAKS WHICH NAMES EXIST");
        Bank generic = bank(new Ticker(), Defences.none().withGenericMessages(), Set.of());
        line(out, "  with one message for both: \"" + generic.login("probe", "zed", "x").message() + "\" and \""
                + generic.login("probe", "emil", "x").message() + "\"");
    }
}
