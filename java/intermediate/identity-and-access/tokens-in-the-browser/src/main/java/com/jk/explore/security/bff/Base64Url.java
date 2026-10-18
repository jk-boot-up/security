package com.jk.explore.security.bff;

import java.util.Base64;

/** Base64url without padding, as JWS uses it (RFC 7515 section 2). Decoding is strict: any other character, or padding, is refused. */
final class Base64Url {

    private Base64Url() {
    }

    static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static byte[] decode(String text) throws InvalidTokenException {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!ok) {
                throw new InvalidTokenException("a part is not base64url");
            }
        }
        if (text.length() % 4 == 1) {
            throw new InvalidTokenException("a part is not base64url");
        }
        return Base64.getUrlDecoder().decode(text);
    }
}
