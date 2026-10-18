package com.jk.explore.security.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Proof Key for Code Exchange (RFC 7636). The client makes a random secret, the code verifier, and sends only its SHA-256 hash, the code
 * challenge, with the authorization request. To redeem the code it must show the verifier itself, which never travels through the browser.
 */
final class Pkce {

    static final String S256 = "S256";

    private static final SecureRandom RANDOM = new SecureRandom();

    private Pkce() {
    }

    /** A fresh verifier: 32 random bytes in base64url, 43 characters, as RFC 7636 section 4.1 recommends. */
    static String newVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return base64Url(bytes);
    }

    /** The S256 challenge: BASE64URL(SHA-256(ASCII(verifier))). */
    static String challenge(String verifier) {
        try {
            return base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    /** A verifier must be 43 to 128 characters from the unreserved set: letters, digits, '-', '.', '_' and '~'. */
    static boolean wellFormed(String verifier) {
        if (verifier == null || verifier.length() < 43 || verifier.length() > 128) {
            return false;
        }
        for (int i = 0; i < verifier.length(); i++) {
            char c = verifier.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_' || c == '~';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Does this verifier belong to that challenge? Compared in constant time. */
    static boolean matches(String challenge, String verifier) {
        if (!wellFormed(verifier)) {
            return false;
        }
        return MessageDigest.isEqual(challenge.getBytes(StandardCharsets.US_ASCII), challenge(verifier).getBytes(StandardCharsets.US_ASCII));
    }

    static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
