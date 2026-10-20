package com.jk.explore.security.tlslog;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** TLS 1.3 record protection: AES-128-GCM, a nonce made from the IV and the record number, and the record header as additional data. */
final class Records {

    private Records() {
    }

    static byte[] seal(byte[] trafficSecret, long seq, String plaintext) {
        byte[] inner = Arrays.copyOf(plaintext.getBytes(StandardCharsets.UTF_8), plaintext.getBytes(StandardCharsets.UTF_8).length + 1);
        inner[inner.length - 1] = 0x17; // the real content type goes inside, so an observer only ever sees "application data"
        int length = inner.length + 16;
        byte[] header = {0x17, 0x03, 0x03, (byte) (length >> 8), (byte) length};
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(trafficSecret), new GCMParameterSpec(128, nonce(trafficSecret, seq)));
            c.updateAAD(header);
            byte[] body = c.doFinal(inner);
            byte[] out = new byte[5 + body.length];
            System.arraycopy(header, 0, out, 0, 5);
            System.arraycopy(body, 0, out, 5, body.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns the text, or null when the key is wrong or the record was changed. */
    static String open(byte[] trafficSecret, long seq, byte[] record) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(trafficSecret), new GCMParameterSpec(128, nonce(trafficSecret, seq)));
            c.updateAAD(record, 0, 5);
            byte[] inner = c.doFinal(record, 5, record.length - 5);
            return new String(inner, 0, inner.length - 1, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private static SecretKeySpec key(byte[] secret) {
        return new SecretKeySpec(Hkdf.expandLabel(secret, "key", new byte[0], 16), "AES");
    }

    private static byte[] nonce(byte[] secret, long seq) {
        byte[] iv = Hkdf.expandLabel(secret, "iv", new byte[0], 12);
        for (int i = 0; i < 8; i++) {
            iv[11 - i] ^= (byte) (seq >> (8 * i));
        }
        return iv;
    }
}
