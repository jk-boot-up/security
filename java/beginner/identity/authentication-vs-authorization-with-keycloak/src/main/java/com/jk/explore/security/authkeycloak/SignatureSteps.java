package com.jk.explore.security.authkeycloak;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Redoes by hand the check the resource server makes on an RS256 token, so each step can be printed.
 * The library does the same; a test proves this class and the library always agree.
 */
final class SignatureSteps {

    /** The 19 bytes that say "what follows is a SHA-256 hash" (the DigestInfo prefix of PKCS #1 v1.5). */
    static final byte[] SHA256_PREFIX = HexFormat.of().parseHex("3031300d060960864801650304020105000420");

    /** What each step gave: the signed text, its hash, the opened signature, and whether they agree. */
    record Steps(String signedText, byte[] hash, byte[] opened, boolean matches) {

        /** The last 32 bytes of the opened signature: the hash the login server signed. */
        byte[] hashInSignature() {
            return Arrays.copyOfRange(opened, opened.length - 32, opened.length);
        }

        /** How many ff padding bytes follow 00 01 at the start of the opened signature. */
        int paddingBytes() {
            int n = 0;
            while (2 + n < opened.length && opened[2 + n] == (byte) 0xff) {
                n++;
            }
            return n;
        }
    }

    private SignatureSteps() {
    }

    /** Step 1: the signed text. Step 2: its SHA-256 hash. Step 3: open the signature with the public key. Step 4: compare. */
    static Steps check(String token, RSAPublicKey key) {
        String[] parts = token.split("\\.");
        String signedText = parts[0] + "." + parts[1];
        byte[] hash = sha256(signedText.getBytes(StandardCharsets.US_ASCII));
        BigInteger signature = new BigInteger(1, Base64.getUrlDecoder().decode(parts[2]));
        BigInteger m = signature.modPow(key.getPublicExponent(), key.getModulus());
        byte[] opened = toBytes(m, (key.getModulus().bitLength() + 7) / 8);
        boolean matches = Arrays.equals(opened, expected(hash, opened.length));
        return new Steps(signedText, hash, opened, matches);
    }

    /** What the opened signature must be: 00 01, ff bytes, 00, the SHA-256 prefix, the hash. */
    static byte[] expected(byte[] hash, int length) {
        byte[] block = new byte[length];
        Arrays.fill(block, (byte) 0xff);
        block[0] = 0x00;
        block[1] = 0x01;
        int tail = length - SHA256_PREFIX.length - hash.length;
        block[tail - 1] = 0x00;
        System.arraycopy(SHA256_PREFIX, 0, block, tail, SHA256_PREFIX.length);
        System.arraycopy(hash, 0, block, tail + SHA256_PREFIX.length, hash.length);
        return block;
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] toBytes(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        byte[] out = new byte[length];
        int copy = Math.min(raw.length, length);
        System.arraycopy(raw, raw.length - copy, out, length - copy, copy);
        return out;
    }

    /** Bytes as hex in groups of four bytes: "0546a381 4647a1ad". */
    static String hex(byte[] bytes, int from, int to) {
        StringBuilder out = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from && (i - from) % 4 == 0) {
                out.append(' ');
            }
            out.append(String.format("%02x", bytes[i]));
        }
        return out.toString();
    }
}
