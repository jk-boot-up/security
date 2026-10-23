package com.jk.explore.security.k8ssecrets;

import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The claims of a ServiceAccount token. Reading them needs no key: a JWT is signed, not encrypted. */
record Jwt(JsonNode claims) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    static Jwt parse(String token) {
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("not a JWT");
        }
        return new Jwt(JSON.readTree(Base64.getUrlDecoder().decode(parts[1])));
    }

    boolean hasExpiry() {
        return claims.has("exp");
    }

    /** Seconds from issue to expiry, or -1 when the token never expires. */
    long lifetimeSeconds() {
        return hasExpiry() ? claims.path("exp").asLong() - claims.path("iat").asLong() : -1;
    }

    boolean hasAudience(String audience) {
        for (JsonNode a : claims.path("aud")) {
            if (a.asString().equals(audience)) {
                return true;
            }
        }
        return false;
    }

    /** The pod the token is tied to, or an empty string. */
    String boundPod() {
        return claims.path("kubernetes.io").path("pod").path("name").asString("");
    }

    String subject() {
        return claims.path("sub").asString("");
    }
}
