package com.jk.explore.security.passkeysspring;

import java.util.Base64;

/** base64url without padding, the encoding WebAuthn uses for every binary value in its JSON. */
final class B64 {

    private B64() {
    }

    static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static byte[] decode(String text) {
        return Base64.getUrlDecoder().decode(text);
    }
}
