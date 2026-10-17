package com.jk.explore.security.authkeycloak;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A stand-in for the login server in the demo: it signs tokens in the shape Keycloak produces (issuer, audience, authorized party,
 * realm_access.roles, expiry). The key is random and never printed. The integration tests use the real Keycloak instead.
 */
final class Issuer {

    static final String REALM_ISSUER = "http://login.shop.example/realms/shop";
    static final String AUDIENCE = "shop-api";
    static final String CLIENT = "shop-web";

    private final KeyPair keys;
    private final String issuer;
    private final Clock clock;
    private int counter;

    Issuer(String issuer, Clock clock) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keys = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        this.issuer = issuer;
        this.clock = clock;
    }

    /** The id of the token most recently signed by this issuer. */
    String lastTokenId() {
        return "token-" + counter;
    }

    RSAPublicKey publicKey() {
        return (RSAPublicKey) keys.getPublic();
    }

    /** A signed token for a user, valid for the given number of seconds from the clock's now. */
    String mint(String user, List<String> roles, long lifetimeSeconds, String audience) {
        return sign(user, roles, lifetimeSeconds, audience, issuer, (RSAPrivateKey) keys.getPrivate());
    }

    String mintWithIssuer(String user, List<String> roles, long lifetimeSeconds, String audience, String otherIssuer) {
        return sign(user, roles, lifetimeSeconds, audience, otherIssuer, (RSAPrivateKey) keys.getPrivate());
    }

    /** A token signed with a different key, as a forger would make. */
    String forge(String user, List<String> roles) {
        Issuer other = new Issuer(issuer, clock);
        return other.mint(user, roles, 300, AUDIENCE);
    }

    private String sign(String user, List<String> roles, long lifetimeSeconds, String audience, String iss, RSAPrivateKey key) {
        long now = clock.instant().getEpochSecond();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(iss)
                .subject(user)
                .audience(audience)
                .claim("azp", CLIENT)
                .claim("preferred_username", user)
                .claim("realm_access", Map.of("roles", roles))
                .issueTime(new Date(now * 1000))
                .expirationTime(new Date((now + lifetimeSeconds) * 1000))
                .jwtID("token-" + (++counter))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
