package com.jk.explore.security.oidc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates an ID token the way OpenID Connect Core 1.0 section 3.1.3.7 requires, with the checks of RFC 8725 on top.
 * The algorithm is fixed here, not read from the token; the key is chosen by id from the provider's published set; and the
 * claims are believed only after the signature has been verified.
 */
final class IdTokenValidator {

    static final long SKEW_SECONDS = 30;
    static final long MAX_AGE_SECONDS = 600;

    private IdTokenValidator() {
    }

    static IdToken validate(String token, ProviderConfig provider, String clientId, String expectedNonce, long now) throws InvalidTokenException {
        Jws.Parts parts = Jws.parse(token);
        if (!"EdDSA".equals(parts.header().get("alg"))) {
            throw new InvalidTokenException("algorithm is not EdDSA");
        }
        Object typ = parts.header().get("typ");
        if (typ != null && !"JWT".equals(typ)) {
            throw new InvalidTokenException("not an ID token (typ " + typ + ")");
        }
        if (!(parts.header().get("kid") instanceof String kid)) {
            throw new InvalidTokenException("no key id");
        }
        PublicKey key = provider.keys().find(kid).orElseThrow(() -> new InvalidTokenException("unknown key id"));
        if (!Ed25519.verify(key, parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("signature does not match");
        }
        Map<String, Object> claims = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));

        if (!provider.issuer().equals(claims.get("iss"))) {
            throw new InvalidTokenException("issuer is not the configured issuer");
        }
        List<String> audience = audience(claims.get("aud"));
        if (!audience.contains(clientId)) {
            throw new InvalidTokenException("audience does not include this client");
        }
        if (audience.size() > 1 && !clientId.equals(claims.get("azp"))) {
            throw new InvalidTokenException("several audiences and no azp naming this client");
        }
        if (claims.containsKey("azp") && !clientId.equals(claims.get("azp"))) {
            throw new InvalidTokenException("azp names another client");
        }
        long exp = number(claims, "exp");
        long iat = number(claims, "iat");
        if (now >= exp + SKEW_SECONDS) {
            throw new InvalidTokenException("expired");
        }
        if (iat > now + SKEW_SECONDS) {
            throw new InvalidTokenException("issued in the future");
        }
        if (now - iat > MAX_AGE_SECONDS) {
            throw new InvalidTokenException("issued too long ago");
        }
        if (!(claims.get("nonce") instanceof String nonce)) {
            throw new InvalidTokenException("no nonce");
        }
        if (!MessageDigest.isEqual(nonce.getBytes(StandardCharsets.UTF_8), expectedNonce.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidTokenException("nonce is not the one this browser was given");
        }
        if (!(claims.get("sub") instanceof String sub) || sub.isEmpty()) {
            throw new InvalidTokenException("no subject");
        }
        List<String> amr = new ArrayList<>();
        if (claims.get("amr") instanceof List<?> list) {
            list.forEach(m -> amr.add(String.valueOf(m)));
        }
        return new IdToken(provider.issuer(), sub, claims.get("email") instanceof String e ? e : null,
                Boolean.TRUE.equals(claims.get("email_verified")), claims.get("sid") instanceof String s ? s : null,
                claims.get("auth_time") instanceof Long t ? t : iat, claims.get("acr") instanceof String a ? a : null, amr);
    }

    static List<String> audience(Object aud) throws InvalidTokenException {
        if (aud instanceof String single) {
            return List.of(single);
        }
        if (aud instanceof List<?> list && !list.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof String s)) {
                    throw new InvalidTokenException("audience is not text");
                }
                out.add(s);
            }
            return out;
        }
        throw new InvalidTokenException("no audience");
    }

    static long number(Map<String, Object> claims, String name) throws InvalidTokenException {
        if (!(claims.get(name) instanceof Long value)) {
            throw new InvalidTokenException("no " + name);
        }
        return value;
    }
}
