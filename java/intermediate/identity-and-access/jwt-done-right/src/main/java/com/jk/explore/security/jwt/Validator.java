package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates the bank's access tokens, in this order: the shape, the algorithm and type the server expects, a key the server already
 * holds, the signature, and only then the claims (expiry, not-before, issuer, audience, scope). The claims are never read before the
 * signature has been checked.
 */
final class Validator {

    private final Policy policy;
    private final KeySet keys;
    private final SimClock clock;

    Validator(Policy policy, KeySet keys, SimClock clock) {
        this.policy = policy;
        this.keys = keys;
        this.clock = clock;
    }

    AccessToken validate(String token) throws InvalidTokenException {
        Jws.Parts parts = Jws.parse(token);
        Map<String, Object> header = parts.header();

        Object alg = header.get("alg");
        if (!Policy.ALGORITHM.equals(alg)) {
            throw new InvalidTokenException("algorithm " + alg + " is not " + Policy.ALGORITHM);
        }
        Object typ = header.get("typ");
        if (!(typ instanceof String t) || !t.toLowerCase(Locale.ROOT).equals(Policy.TYPE)) {
            throw new InvalidTokenException("type " + typ + " is not " + Policy.TYPE);
        }
        if (header.containsKey("crit")) {
            throw new InvalidTokenException("a critical header the API does not understand");
        }
        if (!(header.get("kid") instanceof String kid)) {
            throw new InvalidTokenException("no key id");
        }
        PublicKey key = keys.find(kid).orElseThrow(() -> new InvalidTokenException("unknown key id " + kid));
        if (!Ed25519.verify(key, parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("signature does not match");
        }

        Map<String, Object> claims = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));
        long now = clock.now();
        long exp = number(claims, "exp");
        long iat = number(claims, "iat");
        if (now >= exp + policy.skewSeconds()) {
            throw new InvalidTokenException("expired");
        }
        if (claims.containsKey("nbf") && now + policy.skewSeconds() < number(claims, "nbf")) {
            throw new InvalidTokenException("not valid yet");
        }
        if (iat > now + policy.skewSeconds()) {
            throw new InvalidTokenException("issued in the future");
        }
        if (exp - iat > policy.maxLifetimeSeconds()) {
            throw new InvalidTokenException("lifetime longer than the policy allows");
        }
        if (!policy.issuer().equals(claims.get("iss"))) {
            throw new InvalidTokenException("another issuer");
        }
        if (!audiences(claims.get("aud")).contains(policy.audience())) {
            throw new InvalidTokenException("another audience");
        }
        Set<String> scopes = claims.get("scope") instanceof String s ? Set.of(s.trim().split(" +")) : Set.of();
        if (!scopes.contains(policy.requiredScope())) {
            throw new InvalidTokenException("missing scope " + policy.requiredScope());
        }
        if (!(claims.get("sub") instanceof String subject) || subject.isEmpty()) {
            throw new InvalidTokenException("no subject");
        }
        return new AccessToken(subject, scopes, exp);
    }

    private static long number(Map<String, Object> claims, String name) throws InvalidTokenException {
        if (!(claims.get(name) instanceof Long value)) {
            throw new InvalidTokenException("no " + name + " claim");
        }
        return value;
    }

    private static List<?> audiences(Object aud) {
        if (aud instanceof String s) {
            return List.of(s);
        }
        return aud instanceof List<?> list ? list : List.of();
    }
}
