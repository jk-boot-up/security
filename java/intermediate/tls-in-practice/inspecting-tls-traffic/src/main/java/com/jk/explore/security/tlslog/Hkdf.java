package com.jk.explore.security.tlslog;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HKDF (RFC 5869) with HMAC-SHA-256, and the two TLS 1.3 helpers built on it (RFC 8446, section 7.1). */
final class Hkdf {

    static final byte[] ZEROS = new byte[32];

    private Hkdf() {
    }

    static byte[] extract(byte[] salt, byte[] ikm) {
        return hmac(salt, ikm);
    }

    static byte[] expand(byte[] prk, byte[] info, int length) {
        byte[] out = new byte[length];
        byte[] t = new byte[0];
        int pos = 0;
        for (int i = 1; pos < length; i++) {
            Mac mac = mac(prk);
            mac.update(t);
            mac.update(info);
            mac.update((byte) i);
            t = mac.doFinal();
            int n = Math.min(t.length, length - pos);
            System.arraycopy(t, 0, out, pos, n);
            pos += n;
        }
        return out;
    }

    /** HKDF-Expand-Label: the label is prefixed with "tls13 " and sent with the context and the output length. */
    static byte[] expandLabel(byte[] secret, String label, byte[] context, int length) {
        byte[] full = ("tls13 " + label).getBytes(StandardCharsets.US_ASCII);
        byte[] info = new byte[2 + 1 + full.length + 1 + context.length];
        info[0] = (byte) (length >> 8);
        info[1] = (byte) length;
        info[2] = (byte) full.length;
        System.arraycopy(full, 0, info, 3, full.length);
        info[3 + full.length] = (byte) context.length;
        System.arraycopy(context, 0, info, 4 + full.length, context.length);
        return expand(secret, info, length);
    }

    static byte[] deriveSecret(byte[] secret, String label, byte[] transcriptHash) {
        return expandLabel(secret, label, transcriptHash, 32);
    }

    static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            for (byte[] p : parts) {
                d.update(p);
            }
            return d.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        return mac(key).doFinal(data);
    }

    private static Mac mac(byte[] key) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key.length == 0 ? new byte[32] : key, "HmacSHA256"));
            return m;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
