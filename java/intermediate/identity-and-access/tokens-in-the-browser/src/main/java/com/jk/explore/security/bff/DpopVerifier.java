package com.jk.explore.security.bff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Checks a DPoP proof as RFC 9449 section 4.3 describes: one well-formed JWT of type dpop+jwt, signed with the EdDSA key in its
 * own header, for this method and this URL, made within the last minute, never seen before, and, when an access token comes with
 * it, carrying that token's hash. It returns the thumbprint of the key that signed, for the caller to compare with the token.
 */
final class DpopVerifier {

    static final long WINDOW_SECONDS = 60;

    private final SimClock clock;
    private final Map<String, Long> seen = new HashMap<>();

    DpopVerifier(SimClock clock) {
        this.clock = clock;
    }

    /** The URL a proof names: scheme, host and path, without query or fragment (RFC 9449 section 4.2). */
    static String target(String url) {
        URI uri = URI.create(url);
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getRawAuthority().toLowerCase(Locale.ROOT) + uri.getRawPath();
    }

    String verify(String proof, String method, String url, String accessToken) throws InvalidTokenException {
        if (proof == null) {
            throw new InvalidTokenException("a DPoP proof is required");
        }
        Jws.Parts parts = Jws.parse(proof);
        if (!"dpop+jwt".equals(parts.header().get("typ"))) {
            throw new InvalidTokenException("the proof's type is not dpop+jwt");
        }
        if (!"EdDSA".equals(parts.header().get("alg"))) {
            throw new InvalidTokenException("the proof's algorithm is not the one accepted");
        }
        if (!(parts.header().get("jwk") instanceof Map<?, ?> jwkRaw)) {
            throw new InvalidTokenException("the proof carries no public key");
        }
        Map<String, Object> jwk = new HashMap<>();
        jwkRaw.forEach((k, v) -> jwk.put(String.valueOf(k), v));
        if (!"OKP".equals(jwk.get("kty")) || !"Ed25519".equals(jwk.get("crv")) || !(jwk.get("x") instanceof String x) || jwk.containsKey("d")) {
            throw new InvalidTokenException("the proof's key is not a public Ed25519 key");
        }
        PublicKey key;
        try {
            key = Ed25519.publicKey(Base64Url.decode(x));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("the proof's key is not a public Ed25519 key");
        }
        if (!Ed25519.verify(key, parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("the proof's signature does not match its key");
        }
        Map<String, Object> claims = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));
        if (!method.equals(claims.get("htm")) || !target(url).equals(claims.get("htu"))) {
            throw new InvalidTokenException("the proof was made for another request");
        }
        if (!(claims.get("iat") instanceof Long issuedAt) || Math.abs(clock.now() - issuedAt) > WINDOW_SECONDS) {
            throw new InvalidTokenException("the proof is too old");
        }
        if (!(claims.get("jti") instanceof String id) || id.length() < 16) {
            throw new InvalidTokenException("the proof has no unique id");
        }
        if (accessToken != null) {
            if (!(claims.get("ath") instanceof String ath) || !Bytes.same(ath, Bytes.sha256Base64Url(accessToken))) {
                throw new InvalidTokenException("the proof was made for another access token");
            }
        }
        seen.values().removeIf(at -> clock.now() - at > 2 * WINDOW_SECONDS);
        if (seen.putIfAbsent(id, issuedAt) != null) {
            throw new InvalidTokenException("the proof was already used");
        }
        return DpopKey.thumbprint(jwk);
    }
}
