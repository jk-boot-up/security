package com.jk.explore.security.signaturesbc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A JWS compact signature with EdDSA (RFC 7515, RFC 8037), written by hand: BASE64URL(header) "." BASE64URL(payload) "." BASE64URL(signature).
 * The detached form leaves the payload out ("header..signature"). The verifier accepts only the algorithm it expects: reading the
 * algorithm from the token is how "alg: none" and key-confusion attacks work.
 */
final class Jws {

    static final String HEADER = "{\"alg\":\"EdDSA\"}";

    private Jws() {
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] unb64(String text) {
        return Base64.getUrlDecoder().decode(text);
    }

    static String sign(byte[] seed, byte[] payload) {
        String signingInput = b64(Hex.bytes(HEADER)) + "." + b64(payload);
        byte[] signature = EdDsa.sign25519(seed, signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + b64(signature);
    }

    static String signDetached(byte[] seed, byte[] payload) {
        String full = sign(seed, payload);
        String[] parts = full.split("\\.");
        return parts[0] + ".." + parts[2];
    }

    /** Verifies a token. For a detached token, pass the payload; for a full token pass null. */
    static boolean verify(byte[] publicKey, String token, byte[] detachedPayload) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !Hex.text(unb64(parts[0])).equals(HEADER)) {
                return false;
            }
            String payload = detachedPayload != null ? b64(detachedPayload) : parts[1];
            if (detachedPayload != null && !parts[1].isEmpty()) {
                return false;
            }
            String signingInput = parts[0] + "." + payload;
            return EdDsa.verify25519(publicKey, signingInput.getBytes(StandardCharsets.US_ASCII), unb64(parts[2]));
        } catch (RuntimeException e) {
            return false;
        }
    }

    static byte[] payloadOf(String token) {
        return unb64(token.split("\\.", -1)[1]);
    }
}
