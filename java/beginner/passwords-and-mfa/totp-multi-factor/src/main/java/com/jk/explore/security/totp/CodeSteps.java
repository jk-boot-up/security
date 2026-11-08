package com.jk.explore.security.totp;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Works out a six-digit TOTP code one step at a time, keeping every value in between, so the demo can show how the
 * code is made. A test checks that it gives exactly the code of {@link Totp}.
 */
public final class CodeSteps {

    /** Every value on the way from the secret and the time to the code. */
    public record Steps(long counter, byte[] counterBytes, byte[] hmac, int offset, byte[] picked, int number, String code) {
    }

    private CodeSteps() {
    }

    /**
     * Step 1: the counter is the number of whole 30-second steps. Step 2: HMAC-SHA1 of the counter, as 8 bytes, with the
     * secret. Step 3: the last half-byte of the HMAC picks where to read 4 bytes. Step 4: drop the top bit and keep the
     * last six digits.
     */
    public static Steps at(byte[] secret, long unixSeconds) {
        long counter = unixSeconds / Totp.STEP_SECONDS;
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hmac = hmacSha1(secret, counterBytes);
        int offset = hmac[hmac.length - 1] & 0x0F;
        byte[] picked = new byte[] {hmac[offset], hmac[offset + 1], hmac[offset + 2], hmac[offset + 3]};
        int number = ByteBuffer.wrap(picked).getInt() & 0x7FFFFFFF;
        String code = String.format("%06d", number % 1_000_000);
        return new Steps(counter, counterBytes, hmac, offset, picked, number, code);
    }

    private static byte[] hmacSha1(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is part of every Java runtime", e);
        }
    }
}
