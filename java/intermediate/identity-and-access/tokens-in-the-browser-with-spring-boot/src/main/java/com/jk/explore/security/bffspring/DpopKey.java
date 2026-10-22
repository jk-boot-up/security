package com.jk.explore.security.bffspring;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * The key that the single-page app creates in the browser and never lets out (in a real browser, a non-extractable WebCrypto key). It signs a proof for each request:
 * which method, which address, when, and, once there is a token, which token.
 */
public final class DpopKey {

    private final ECKey key;

    public DpopKey() {
        try {
            this.key = new ECKeyGenerator(Curve.P_256).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The thumbprint the access token carries in cnf.jkt. */
    public String thumbprint() {
        try {
            return key.computeThumbprint().toString();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A proof for one request. {@code accessToken} is null at the token endpoint and the token itself when calling the API. */
    public String proof(String method, String url, String accessToken, Instant issuedAt, String jti) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().jwtID(jti).claim("htm", method).claim("htu", url).issueTime(Date.from(issuedAt));
            if (accessToken != null) {
                claims.claim("ath", Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(accessToken.getBytes(StandardCharsets.US_ASCII))));
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dpop+jwt")).jwk(key.toPublicJWK()).build(), claims.build());
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (JOSEException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String proof(String method, String url, String accessToken) {
        return proof(method, url, accessToken, Instant.now(), UUID.randomUUID().toString());
    }
}
