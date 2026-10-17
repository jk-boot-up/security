package com.jk.explore.security.integrity;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA-256: a hash that needs a secret key. Only someone who has the key can make the tag for a message, so a tag that
 * checks out shows the message came from a key holder and was not changed.
 */
public final class Hmac {

    private Hmac() {
    }

    public static byte[] tag(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every Java runtime", e);
        }
    }

    public static String hex(byte[] key, byte[] message) {
        return Hex.of(tag(key, message));
    }

    /** Checks a tag, taking the same time wherever the first difference is. */
    public static boolean verify(byte[] key, byte[] message, byte[] tag) {
        return MessageDigest.isEqual(tag(key, message), tag);
    }
}
