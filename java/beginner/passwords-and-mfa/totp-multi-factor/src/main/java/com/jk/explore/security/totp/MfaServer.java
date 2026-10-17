package com.jk.explore.security.totp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bank's login with an optional second factor. A customer who has enrolled must give a code from the authenticator
 * app as well as the password. The server accepts the code for the current 30-second step and for {@code window} steps
 * either side, to forgive clocks that are slightly out, and can remember the last step it accepted so that a code cannot
 * be used twice.
 */
public class MfaServer {

    private final SecretSource secrets;
    private final int window;
    private final boolean replayProtection;
    private final Map<String, String> passwordHashes = new HashMap<>();
    private final Map<String, byte[]> totpSecrets = new HashMap<>();
    private final Map<String, Long> lastAcceptedStep = new HashMap<>();
    private final Map<String, Set<String>> recoveryCodes = new HashMap<>();

    public MfaServer(SecretSource secrets, int window, boolean replayProtection) {
        this.secrets = secrets;
        this.window = window;
        this.replayProtection = replayProtection;
    }

    public MfaServer register(String username, String password) {
        passwordHashes.put(username, hash(password));
        return this;
    }

    /** Gives the customer a new shared secret, as Base32 text for the authenticator app. The server keeps a copy. */
    public String enroll(String username) {
        byte[] secret = secrets.newSecret();
        totpSecrets.put(username, secret);
        return Base32.encode(secret);
    }

    /** Issues single-use recovery codes, for a customer who has lost the phone. */
    public List<String> issueRecoveryCodes(String username, int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(secrets.newRecoveryCode());
        }
        recoveryCodes.put(username, new HashSet<>(codes));
        return codes;
    }

    /** What an attacker who copies the server's table gets: every customer's secret. */
    public Map<String, byte[]> stolenSecrets() {
        return new HashMap<>(totpSecrets);
    }

    /** A login. {@code code} is null when the customer has not given one yet. {@code now} is the true time in seconds. */
    public Verdict login(String username, String password, String code, long now) {
        String stored = passwordHashes.get(username);
        if (stored == null || !MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), hash(password).getBytes(StandardCharsets.UTF_8))) {
            return Verdict.WRONG_PASSWORD;
        }
        byte[] secret = totpSecrets.get(username);
        if (secret == null) {
            return Verdict.OK;
        }
        if (code == null) {
            return Verdict.CODE_NEEDED;
        }
        long current = Totp.counterAt(now);
        for (long step = current - window; step <= current + window; step++) {
            if (step >= 0 && MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),
                    Hotp.code(secret, step, Totp.DIGITS).getBytes(StandardCharsets.UTF_8))) {
                if (replayProtection && step <= lastAcceptedStep.getOrDefault(username, -1L)) {
                    return Verdict.REPLAYED;
                }
                lastAcceptedStep.put(username, step);
                return Verdict.OK;
            }
        }
        return Verdict.WRONG_CODE;
    }

    /** A login with a recovery code instead of a code from the app. Each recovery code works once. */
    public Verdict loginWithRecoveryCode(String username, String password, String recoveryCode) {
        Verdict first = login(username, password, null, 0);
        if (first != Verdict.CODE_NEEDED && first != Verdict.OK) {
            return first;
        }
        Set<String> left = recoveryCodes.getOrDefault(username, Set.of());
        if (left.remove(recoveryCode)) {
            return Verdict.OK;
        }
        return Verdict.WRONG_CODE;
    }

    private static String hash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(("demo-salt:" + password).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
