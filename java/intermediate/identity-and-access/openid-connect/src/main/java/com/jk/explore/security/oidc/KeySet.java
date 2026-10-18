package com.jk.explore.security.oidc;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The provider's public keys, read from a JWKS document (RFC 7517) and looked up by key id. Only Ed25519 signing keys are kept. */
final class KeySet {

    private final Map<String, PublicKey> keys;

    private KeySet(Map<String, PublicKey> keys) {
        this.keys = keys;
    }

    static KeySet fromJwks(Map<String, Object> jwks) throws InvalidTokenException {
        if (!(jwks.get("keys") instanceof List<?> list)) {
            throw new InvalidTokenException("the key set has no keys");
        }
        Map<String, PublicKey> keys = new LinkedHashMap<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> jwk)) {
                continue;
            }
            if (!"OKP".equals(jwk.get("kty")) || !"Ed25519".equals(jwk.get("crv")) || !(jwk.get("kid") instanceof String kid)
                    || !(jwk.get("x") instanceof String x) || (jwk.get("use") != null && !"sig".equals(jwk.get("use")))) {
                continue;
            }
            keys.put(kid, Ed25519.publicKey(Base64Url.decode(x)));
        }
        return new KeySet(keys);
    }

    Optional<PublicKey> find(String kid) {
        return Optional.ofNullable(keys.get(kid));
    }

    List<String> ids() {
        return List.copyOf(keys.keySet());
    }
}
