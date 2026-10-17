package com.jk.explore.security.signatures;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The shop's price list tagged with HMAC, as in the hashing-and-integrity project. Everyone who can check a tag holds the same
 * key, so everyone who can check a tag can also make one.
 */
public class HmacPublisher {

    private final byte[] key;

    public HmacPublisher(byte[] key) {
        this.key = key.clone();
    }

    public byte[] tag(PriceList list) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(list.encode());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean accepts(PriceList list, byte[] tag) {
        return MessageDigest.isEqual(tag(list), tag);
    }
}
