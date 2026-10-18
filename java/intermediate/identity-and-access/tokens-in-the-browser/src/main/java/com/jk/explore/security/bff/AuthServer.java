package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The bank's authorization server, reduced to what this lesson needs: it issues short access tokens (signed JWTs, five minutes)
 * and long refresh tokens (opaque, thirty days). A token can be bound to a client key: the access token then carries the key's
 * thumbprint in {@code cnf.jkt} (RFC 9449 section 6), and the refresh token is refused unless the same key proves possession.
 */
final class AuthServer {

    static final String ISSUER = "https://login.bank.example";
    static final String AUDIENCE = "https://api.bank.example";
    static final long ACCESS_SECONDS = 300;
    static final long REFRESH_SECONDS = 30L * 24 * 3600;

    record Tokens(String accessToken, String refreshToken) {
    }

    private record Grant(String subject, String scope, String keyThumbprint, long expiresAt) {
    }

    private final SimClock clock;
    private final KeyPair signingKey;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Grant> refreshTokens = new HashMap<>();
    private long serial;

    AuthServer(SimClock clock, KeyPair signingKey) {
        this.clock = clock;
        this.signingKey = signingKey;
    }

    PublicKey publicKey() {
        return signingKey.getPublic();
    }

    /** Tokens for a signed-in user. {@code keyThumbprint} is null for plain bearer tokens. */
    Tokens issue(String subject, String scope, String keyThumbprint) {
        String refresh = randomToken();
        refreshTokens.put(refresh, new Grant(subject, scope, keyThumbprint, clock.now() + REFRESH_SECONDS));
        return new Tokens(accessToken(subject, scope, keyThumbprint), refresh);
    }

    /**
     * A new access token for a refresh token. {@code proofThumbprint} is the thumbprint of the key that signed the request's DPoP
     * proof, already verified by the caller, or null when there was no proof. The refresh token is rotated on every use.
     */
    Optional<Tokens> refresh(String refreshToken, String proofThumbprint) {
        Grant grant = refreshTokens.get(refreshToken);
        if (grant == null || clock.now() >= grant.expiresAt()) {
            return Optional.empty();
        }
        if (grant.keyThumbprint() != null && (proofThumbprint == null || !Bytes.same(grant.keyThumbprint(), proofThumbprint))) {
            return Optional.empty();
        }
        refreshTokens.remove(refreshToken);
        String next = randomToken();
        refreshTokens.put(next, grant);
        return Optional.of(new Tokens(accessToken(grant.subject(), grant.scope(), grant.keyThumbprint()), next));
    }

    private String accessToken(String subject, String scope, String keyThumbprint) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "EdDSA");
        header.put("typ", "at+jwt");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", subject);
        claims.put("aud", AUDIENCE);
        claims.put("scope", scope);
        claims.put("iat", clock.now());
        claims.put("exp", clock.now() + ACCESS_SECONDS);
        claims.put("jti", "at-" + (++serial));
        if (keyThumbprint != null) {
            claims.put("cnf", Map.of("jkt", keyThumbprint));
        }
        return Jws.sign(header, Json.write(claims).getBytes(StandardCharsets.UTF_8), signingKey.getPrivate());
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64Url.encode(bytes);
    }
}
