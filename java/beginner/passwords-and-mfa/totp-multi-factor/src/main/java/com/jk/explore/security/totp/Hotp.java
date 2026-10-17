package com.jk.explore.security.totp;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HOTP (RFC 4226): a code made from a secret key and a counter. The counter is written as 8 bytes, run through HMAC with
 * the key, and a few bytes of the result, picked by its last digit, are turned into a short number.
 */
public final class Hotp {

    private Hotp() {
    }

    /** The code for this counter, as {@code digits} digits with leading zeros. */
    public static String code(byte[] key, long counter, int digits, String hmacAlgorithm) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(key, hmacAlgorithm));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            int modulus = 1;
            for (int i = 0; i < digits; i++) {
                modulus *= 10;
            }
            return String.format("%0" + digits + "d", binary % modulus);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(hmacAlgorithm + " is part of every Java runtime", e);
        }
    }

    public static String code(byte[] key, long counter, int digits) {
        return code(key, counter, digits, "HmacSHA1");
    }
}
