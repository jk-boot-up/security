package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The bank's authorization server: it holds the signing keys, signs access tokens with the current one, and says what it publishes. */
final class BankIssuer {

    static final String ISSUER = "https://auth.bank.example";
    static final String ACCOUNTS_API = "accounts-api";
    static final String LOYALTY_API = "loyalty-api";
    static final JOSEObjectType ACCESS_TOKEN = new JOSEObjectType("at+jwt");

    private final Clock clock;
    private final Map<String, ECKey> keys = new LinkedHashMap<>();
    private String signingKey;

    BankIssuer(Clock clock) {
        this.clock = clock;
    }

    /** Creates a P-256 key pair with the given id and keeps it, without publishing or using it yet. */
    ECKey createKey(String kid) throws JOSEException {
        ECKey key = new ECKeyGenerator(Curve.P_256).keyID(kid).generate();
        keys.put(kid, key);
        return key;
    }

    void signWith(String kid) {
        this.signingKey = kid;
    }

    void retire(String kid) {
        keys.remove(kid);
    }

    ECKey key(String kid) {
        return keys.get(kid);
    }

    /** The ids of the keys the bank still holds. */
    List<String> keyIds() {
        return List.copyOf(keys.keySet());
    }

    /** The public halves, which is what the bank publishes. */
    List<ECKey> publicKeys() {
        return keys.values().stream().map(ECKey::toPublicJWK).toList();
    }

    String accessToken(String subject, String scope) throws JOSEException {
        return accessToken(subject, scope, ACCESS_TOKEN, ISSUER, ACCOUNTS_API, Duration.ofMinutes(5), Duration.ZERO, signingKey);
    }

    /** The same, with every claim and header field that the demo wants to get wrong. */
    String accessToken(String subject, String scope, JOSEObjectType type, String issuer, String audience, Duration lifetime, Duration notBeforeOffset, String kid) throws JOSEException {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience(audience).claim("scope", scope)
                .issueTime(Date.from(now)).notBeforeTime(Date.from(now.plus(notBeforeOffset))).expirationTime(Date.from(now.plus(lifetime))).build();
        return sign(claims, type, kid, keys.get(kid));
    }

    /** Signs claims with any key, and puts the given key id in the header. */
    static String sign(JWTClaimsSet claims, JOSEObjectType type, String kid, ECKey key) throws JOSEException {
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.ES256).type(type);
        if (kid != null) {
            header.keyID(kid);
        }
        return sign(claims, header.build(), new ECDSASigner(key));
    }

    static String sign(JWTClaimsSet claims, JWSHeader header, JWSSigner signer) throws JOSEException {
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        return jwt.serialize();
    }
}
