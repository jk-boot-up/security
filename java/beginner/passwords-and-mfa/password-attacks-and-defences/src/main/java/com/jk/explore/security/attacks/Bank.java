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

        Account account = accounts.get(username);
        if (account == null) {
            return new Reply(Outcome.UNKNOWN_USER, defences.genericMessages()
                    ? "wrong username or password" : "no account with that name");
        }

        String key = defences.lockScope() == LockScope.ACCOUNT ? username : username + "@" + address;
        if (defences.lockoutAfter() > 0 && now < lockedUntilByKey.getOrDefault(key, 0)) {
            return new Reply(Outcome.LOCKED, "account locked, try again later");
        }

        if (!hash(username, password).equals(account.passwordHash)) {
            int failures = failuresByKey.merge(key, 1, Integer::sum);
            if (defences.lockoutAfter() > 0 && failures >= defences.lockoutAfter()) {
                lockedUntilByKey.put(key, now + 1 + defences.lockoutTicks());
                failuresByKey.put(key, 0);
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

    private static String hash(String username, String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("demo-salt:" + username + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
