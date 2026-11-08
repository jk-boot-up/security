package com.jk.explore.security.totpotp;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Works out a six-digit TOTP code by hand, step by step, so every value the library hides can be shown. It is for
 * teaching only: a test checks it gives the library's exact code, and real code uses the library.
 */
final class TotpSteps {

    /** Every value on the way from a time to a code. */
    record Steps(long step, byte[] counter, byte[] hmac, int offset, int fourBytes, int number, String code) {
    }

    private TotpSteps() {
    }

    /**
     * Step 1: the 30-second step, as 8 bytes. Step 2: HMAC-SHA1 of them with the key, 20 bytes. Step 3: the last
     * half-byte says where to take 4 bytes. Step 4: drop the top bit and keep the last 6 digits.
     */
    static Steps at(byte[] key, long unixSeconds) {
        long step = unixSeconds / 30;
        byte[] counter = ByteBuffer.allocate(8).putLong(step).array();
        byte[] hmac = hmacSha1(key, counter);
        int offset = hmac[hmac.length - 1] & 0x0f;
        int fourBytes = ByteBuffer.wrap(hmac, offset, 4).getInt();
        int number = fourBytes & 0x7fffffff;
        String code = String.format("%06d", number % 1_000_000);
        return new Steps(step, counter, hmac, offset, fourBytes, number, code);
    }

    private static byte[] hmacSha1(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is part of every Java runtime", e);
        }
    }
}
