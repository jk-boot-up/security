package com.jk.explore.security.csrfspring;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Spring Security's masking of the CSRF token (XorCsrfTokenRequestAttributeHandler), redone by hand to show its steps. A page gets
 * random bytes followed by (token XOR random bytes), written as Base64 URL text, so the same token looks different on every page.
 * The tests prove that a token masked here is accepted by Spring, and that unmasking two pages gives the same session token.
 */
final class TokenMask {

    private TokenMask() {
    }

    static byte[] xor(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }

    /** random bytes, then token XOR random bytes, as Base64 URL text. */
    static String mask(String token, byte[] random) {
        byte[] plain = token.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[plain.length * 2];
        System.arraycopy(random, 0, all, 0, plain.length);
        System.arraycopy(xor(plain, random), 0, all, plain.length, plain.length);
        return Base64.getUrlEncoder().encodeToString(all);
    }

    /** The first half XOR the second half gives the token back. */
    static String unmask(String masked) {
        byte[] all = Base64.getUrlDecoder().decode(masked);
        int half = all.length / 2;
        byte[] random = new byte[half];
        byte[] xored = new byte[half];
        System.arraycopy(all, 0, random, 0, half);
        System.arraycopy(all, half, xored, 0, half);
        return new String(xor(xored, random), StandardCharsets.UTF_8);
    }
}
