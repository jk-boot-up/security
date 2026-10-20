package com.jk.explore.security.tokens;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Map;

/**
 * A DPoP proof (RFC 9449): a small signed statement "I, the holder of this key, am sending this method to this address now, with this token",
 * made fresh for each request. {@link #create} is what the client does; {@link ProofCheck} is what the API does.
 */
public final class DpopProof {

    private DpopProof() {
    }

    /** Signs a proof for one request. {@code accessToken} may be null before a token exists. */
    public static String create(KeyPair key, String method, String url, String accessToken, long iat, String jti) {
        String header = "{\"typ\":\"dpop+jwt\",\"alg\":\"ES256\",\"jwk\":" + Jwk.json((ECPublicKey) key.getPublic()) + "}";
        String payload = "{\"jti\":\"" + jti + "\",\"htm\":\"" + method + "\",\"htu\":\"" + url + "\",\"iat\":" + iat
                + (accessToken == null ? "" : ",\"ath\":\"" + hash(accessToken) + "\"") + "}";
        String signingInput = B64.encode(header) + "." + B64.encode(payload);
        try {
            Signature s = Signature.getInstance("SHA256withECDSAinP1363Format");
            s.initSign(key.getPrivate());
            s.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + B64.encode(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hash(String token) {
        try {
            return B64.encode(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** What the API learned from a valid proof: the thumbprint of the key that signed it. */
    public record Verified(String thumbprint) {
    }

    /** The API's checks. Each failure has its own message for the security log. */
    public static final class ProofCheck {

        private static final long WINDOW_SECONDS = 60;

        private final SimClock clock;
        private final Map<String, Long> seen = new java.util.HashMap<>();

        public ProofCheck(SimClock clock) {
            this.clock = clock;
        }

        public Verified verify(String proof, String method, String url, String accessToken) {
            String[] parts = proof == null ? new String[0] : proof.split("\\.", -1);
            if (parts.length != 3) {
                throw new Rejected("not a JWT");
            }
            Map<String, Object> header;
            Map<String, Object> payload;
            try {
                header = MiniJson.parseObject(B64.decodeText(parts[0]));
                payload = MiniJson.parseObject(B64.decodeText(parts[1]));
            } catch (RuntimeException e) {
                throw new Rejected("unreadable proof");
            }
            if (!"dpop+jwt".equals(header.get("typ")) || !"ES256".equals(header.get("alg"))) {
                throw new Rejected("wrong typ or alg");
            }
            if (!(header.get("jwk") instanceof Map<?, ?> jwkRaw)) {
                throw new Rejected("no key in the proof");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> jwk = (Map<String, Object>) jwkRaw;
            PublicKey key;
            try {
                key = Jwk.publicKey(jwk);
                Signature s = Signature.getInstance("SHA256withECDSAinP1363Format");
                s.initVerify(key);
                s.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
                if (!s.verify(B64.decode(parts[2]))) {
                    throw new Rejected("bad signature");
                }
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                throw new Rejected("bad signature");
            }
            if (!method.equals(payload.get("htm"))) {
                throw new Rejected("proof is for another method");
            }
            if (!url.equals(payload.get("htu"))) {
                throw new Rejected("proof is for another address");
            }
            if (!(payload.get("iat") instanceof Long iat) || Math.abs(clock.now() - iat) > WINDOW_SECONDS) {
                throw new Rejected("proof is too old or from the future");
            }
            if (accessToken != null && !hash(accessToken).equals(payload.get("ath"))) {
                throw new Rejected("proof is for another token");
            }
            if (!(payload.get("jti") instanceof String jti) || jti.isEmpty()) {
                throw new Rejected("no jti");
            }
            seen.values().removeIf(exp -> exp < clock.now());
            if (seen.putIfAbsent(jti, clock.now() + 2 * WINDOW_SECONDS) != null) {
                throw new Rejected("proof already used");
            }
            return new Verified(Jwk.thumbprint((String) jwk.get("x"), (String) jwk.get("y")));
        }
    }

    /** A proof was refused. */
    public static final class Rejected extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public Rejected(String message) {
            super(message);
        }
    }
}
