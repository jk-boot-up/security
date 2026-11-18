package com.jk.explore.security.pqc;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A teaching class: HKDF-SHA256 (RFC 5869) redone by hand with HMAC, for a 32-byte key and no salt.
 * The JDK's KDF hides these two steps; a test checks that this gives its exact result.
 */
final class HkdfSteps {

    private HkdfSteps() {
    }

    /** Extract: the pseudo-random key, HMAC-SHA256 keyed with a salt of 32 zero bytes over the input key material. */
    static byte[] extract(byte[] inputKeyMaterial) throws GeneralSecurityException {
        return hmac(new byte[32], inputKeyMaterial);
    }

    /** Expand to 32 bytes: one block, T(1) = HMAC-SHA256(prk, info || 0x01). */
    static byte[] expand(byte[] prk, byte[] info) throws GeneralSecurityException {
        byte[] message = Arrays.copyOf(info, info.length + 1);
        message[info.length] = 1;
        return hmac(prk, message);
    }

    private static byte[] hmac(byte[] key, byte[] message) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }
}
