package com.jk.explore.security.zerotrust;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * A teaching class: an RS256 signature check redone by hand, the steps that Signature "SHA256withRSA" hides (RSASSA-PKCS1-v1_5, RFC 8017).
 * The proxy uses the JDK; a test proves these steps give the JDK's answer.
 */
final class Rs256Steps {

    /** The bytes that say "a SHA-256 hash of 32 bytes follows" (the DigestInfo prefix of RFC 8017, section 9.2). */
    static final byte[] SHA256_LABEL = HexFormat.of().parseHex("3031300d060960864801650304020105000420");

    /** What each step found. The signature is good only when the padding, the label and the hash are all right. */
    record Steps(int signatureBytes, int keyBits, BigInteger e, int blockBytes, int ffBytes, boolean padding, boolean label, boolean hash) {

        boolean valid() {
            return padding && label && hash;
        }
    }

    private Rs256Steps() {
    }

    static Steps check(String token, RSAPublicKey key) throws GeneralSecurityException {
        int dot = token.lastIndexOf('.');
        byte[] signed = token.substring(0, dot).getBytes(StandardCharsets.US_ASCII);   // header.claims, as sent
        byte[] s = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        int k = (key.getModulus().bitLength() + 7) / 8;
        // m = s^e mod n: the public key undoes what the private key did
        byte[] m = fixed(new BigInteger(1, s).modPow(key.getPublicExponent(), key.getModulus()), k);
        int i = 2;
        while (i < k && m[i] == (byte) 0xFF) {
            i++;
        }
        // 00 01, at least 8 bytes FF, then 00
        boolean padding = m[0] == 0 && m[1] == 1 && i - 2 >= 8 && i < k && m[i] == 0;
        int at = i + 1;
        boolean label = padding && k - at == SHA256_LABEL.length + 32 && Arrays.equals(Arrays.copyOfRange(m, at, at + SHA256_LABEL.length), SHA256_LABEL);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(signed);
        boolean hash = label && MessageDigest.isEqual(Arrays.copyOfRange(m, k - 32, k), digest);
        return new Steps(s.length, key.getModulus().bitLength(), key.getPublicExponent(), k, padding ? i - 2 : 0, padding, label, hash);
    }

    /** The number as exactly k bytes, big-endian. */
    private static byte[] fixed(BigInteger v, int k) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[k];
        int n = Math.min(raw.length, k);
        System.arraycopy(raw, raw.length - n, out, k - n, n);
        return out;
    }
}
