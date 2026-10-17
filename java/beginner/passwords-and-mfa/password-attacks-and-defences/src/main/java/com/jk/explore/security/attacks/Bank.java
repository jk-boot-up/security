package com.jk.explore.security.attacks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * The login of an online bank, with the defences against guessing that {@link Defences} switches on. Passwords are
 * kept as salted hashes (see the password-hashing project for how to do that properly); this class is about what
 * happens around the check: lockouts, limits, second factors and messages.
 */
public class Bank {

    private static final class Account {
        final String passwordHash;
        final boolean secondFactor;
        int failures;
        int lockedUntil;

        Account(String passwordHash, boolean secondFactor) {
            this.passwordHash = passwordHash;
            this.secondFactor = secondFactor;
        }
    }

    private final Ticker ticker;
    private final Defences defences;
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Integer> failuresByKey = new HashMap<>();
    private final Map<String, Integer> lockedUntilByKey = new HashMap<>();
    private final Map<String, Integer> attemptsByWindow = new HashMap<>();
    private int hashesComputed;

    public Bank(Ticker ticker, Defences defences) {
        this.ticker = ticker;
        this.defences = defences;
    }

    public Bank open(String username, String password, boolean secondFactor) {
        accounts.put(username, new Account(hash(username, password), secondFactor));
        return this;
    }

    public Set<String> usernames() {
        return Set.copyOf(accounts.keySet());
    }

    /** One login attempt from one address. */
    public Reply login(String address, String username, String password) {
        int now = ticker.now();

        if (defences.perAddressLimit() > 0) {
            String window = address + "@" + (now / defences.windowTicks());
            int used = attemptsByWindow.merge(window, 1, Integer::sum);
            if (used > defences.perAddressLimit()) {
                return new Reply(Outcome.RATE_LIMITED, "too many attempts, slow down");
            }
        }

        String key = defences.lockScope() == LockScope.ACCOUNT ? username : username + "@" + address;
        if (defences.lockoutAfter() > 0 && now < lockedUntilByKey.getOrDefault(key, 0)) {
            return new Reply(Outcome.LOCKED, "account locked, try again later");
        }

        // An unknown name costs the same hash as a known one, and counts toward the same lock: otherwise the time a reply
        // takes, and which names can be locked, tell an attacker which names exist even when the message does not.
        Account account = accounts.get(username);
        String expected = account == null ? UNKNOWN_ACCOUNT_HASH : account.passwordHash;
        boolean matches = hash(username, password).equals(expected) && account != null;
        if (!matches) {
            int failures = failuresByKey.merge(key, 1, Integer::sum);
            if (defences.lockoutAfter() > 0 && failures >= defences.lockoutAfter()) {
                lockedUntilByKey.put(key, now + 1 + defences.lockoutTicks());
                failuresByKey.put(key, 0);
            }
            if (account == null) {
                return new Reply(Outcome.UNKNOWN_USER, defences.genericMessages()
                        ? "wrong username or password" : "no account with that name");
            }
            return new Reply(Outcome.WRONG_PASSWORD, defences.genericMessages()
                    ? "wrong username or password" : "wrong password");
        }

        failuresByKey.put(key, 0);
        if (defences.breached().contains(password)) {
            return new Reply(Outcome.RESET_REQUIRED, "this password is known from a breach: choose a new one");
        }
        if (account.secondFactor) {
            return new Reply(Outcome.SECOND_FACTOR_NEEDED, "enter the code from your device");
        }
        return new Reply(Outcome.OK, "welcome");
    }

    /** How many password hashes this bank has computed: one per login attempt that reaches the check, known name or not. */
    public int hashesComputed() {
        return hashesComputed;
    }

    /** Compared against for names that have no account, so that they take the same work as names that do. */
    private static final String UNKNOWN_ACCOUNT_HASH = "0".repeat(64);

    private String hash(String username, String password) {
        hashesComputed++;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("demo-salt:" + username + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
