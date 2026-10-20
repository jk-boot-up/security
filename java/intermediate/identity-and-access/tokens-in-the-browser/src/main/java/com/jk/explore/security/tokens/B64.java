package com.jk.explore.security.tokens;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Base64url without padding, the encoding every JOSE part uses. */
final class B64 {

    private B64() {
    }

    static String encode(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String encode(String s) {
        return encode(s.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] decode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    static String decodeText(String s) {
        return new String(decode(s), StandardCharsets.UTF_8);
    }
}
