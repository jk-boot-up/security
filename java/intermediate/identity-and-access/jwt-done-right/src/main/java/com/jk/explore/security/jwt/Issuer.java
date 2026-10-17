package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;

/** The bank's authorization server, reduced to the part that signs access tokens. */
final class Issuer {

    static final String ISSUER = "https://auth.bank.example";

    private Issuer() {
    }

    static Map<String, Object> header(String kid) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", Policy.ALGORITHM);
        header.put("typ", Policy.TYPE);
        header.put("kid", kid);
        return header;
    }

    static Map<String, Object> claims(String subject, String scope, long issuedAt, long lifetime) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", subject);
        claims.put("aud", "accounts-api");
        claims.put("scope", scope);
        claims.put("iat", issuedAt);
        claims.put("nbf", issuedAt);
        claims.put("exp", issuedAt + lifetime);
        return claims;
    }

    static String issue(PrivateKey key, Map<String, ?> header, Map<String, ?> claims) {
        return Jws.sign(header, Json.write(claims).getBytes(StandardCharsets.UTF_8), key);
    }

    static String issue(String kid, PrivateKey key, String subject, String scope, long issuedAt, long lifetime) {
        return issue(key, header(kid), claims(subject, scope, issuedAt, lifetime));
    }
}
