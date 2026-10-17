package com.jk.explore.security.totpotp;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * The bank's check of a one-time code, built on the java-otp library. The library makes a code and can say whether a code
 * is right for exactly one time step. Everything else a bank needs is written here: a window of steps either side, and a
 * memory of the last step accepted so that a code cannot be used twice.
 */
public class OtpService {

    /** The bank's answer to a code. */
    public enum Result {
        OK,
        WRONG_CODE,
        REPLAYED
    }

    private final TimeBasedOneTimePasswordGenerator generator;
    private final int window;
    private final boolean replayProtection;
    private final Map<String, Long> lastStep = new HashMap<>();

    public OtpService(TimeBasedOneTimePasswordGenerator generator, int window, boolean replayProtection) {
        this.generator = generator;
        this.window = window;
        this.replayProtection = replayProtection;
    }

    /** A service with the usual settings: SHA-1, 6 digits, 30-second steps. */
    public static OtpService standard(int window, boolean replayProtection) {
        return new OtpService(new TimeBasedOneTimePasswordGenerator(), window, replayProtection);
    }

    /** The code the library makes for this key at this many seconds since 1970. */
    public String codeAt(SecretKey key, long unixSeconds) {
        try {
            return generator.generateOneTimePasswordString(key, Instant.ofEpochSecond(unixSeconds));
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** The library's own check: is this code right for the single step that contains {@code unixSeconds}? */
    public boolean libraryValidates(SecretKey key, long unixSeconds, String code) {
        try {
            return generator.validateOneTimePassword(key, Instant.ofEpochSecond(unixSeconds), code);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Checks a code for one customer: right for this step or a step within the window, and not used before. */
    public Result verify(String user, SecretKey key, String code, long now) {
        long stepSeconds = generator.getTimeStep().toSeconds();
        long current = now / stepSeconds;
        for (long step = current - window; step <= current + window; step++) {
            if (step >= 0 && libraryValidates(key, step * stepSeconds, code)) {
                if (replayProtection && step <= lastStep.getOrDefault(user, -1L)) {
                    return Result.REPLAYED;
                }
                lastStep.put(user, step);
                return Result.OK;
            }
        }
        return Result.WRONG_CODE;
    }

    /** A generator for another hash and length, such as the 8-digit SHA-256 codes some systems use. */
    public static TimeBasedOneTimePasswordGenerator generator(int digits, String algorithm) {
        try {
            return new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), digits, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is part of every Java runtime", e);
        }
    }
}
