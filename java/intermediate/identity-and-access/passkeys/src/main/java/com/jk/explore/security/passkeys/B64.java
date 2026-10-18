package com.jk.explore.security.passkeys;

import java.util.Base64;

/** Base64url without padding (RFC 4648 section 5), the encoding WebAuthn uses for challenges and credential ids. Decoding is strict. */
final class B64 {

    private B64() {
    }

    static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static byte[] decode(String text) {
        if (!text.matches("[A-Za-z0-9_-]*") || text.length() % 4 == 1) {
            throw new IllegalArgumentException("not base64url without padding");
        }
        return Base64.getUrlDecoder().decode(text);
    }
}
