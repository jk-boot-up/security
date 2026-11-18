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
import java.security.SecureRandom;
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
        this(new SecureRandom());
    }

    private DpopKey(SecureRandom random) {
        try {
            this.key = new ECKeyGenerator(Curve.P_256).secureRandom(random).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A key made from a fixed seed, so the demo prints the same thumbprint on every run. A real browser makes a random one. */
    public static DpopKey seeded(String label) {
        return new DpopKey(new FixedBytes(sha256(label.getBytes(StandardCharsets.UTF_8))));
    }

    /** The public key as RFC 7638 hashes it: crv, kty, x and y, in that (alphabetical) order, with no spaces. */
    public String canonical() {
        return "{\"crv\":\"" + key.getCurve().getName() + "\",\"kty\":\"EC\""
                + ",\"x\":\"" + key.getX() + "\",\"y\":\"" + key.getY() + "\"}";
    }

    /** The thumbprint worked out by hand: SHA-256 of the canonical text, in base64url. A test shows it equals the library's. */
    public String thumbprintByHand() {
        byte[] hash = sha256(canonical().getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Hands out the same bytes every time, so the key generator makes the same key from the same seed. */
    private static final class FixedBytes extends SecureRandom {

        private static final long serialVersionUID = 1L;
        private final byte[] seed;

        FixedBytes(byte[] seed) {
            this.seed = seed.clone();
        }

        @Override
        public void nextBytes(byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = seed[i % seed.length];
            }
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
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().jwtID(jti)
                    .claim("htm", method).claim("htu", url).issueTime(Date.from(issuedAt));
            if (accessToken != null) {
                byte[] hash = sha256(accessToken.getBytes(StandardCharsets.US_ASCII));
                claims.claim("ath", Base64.getUrlEncoder().withoutPadding().encodeToString(hash));
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dpop+jwt")).jwk(key.toPublicJWK()).build(), claims.build());
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public String proof(String method, String url, String accessToken) {
        return proof(method, url, accessToken, Instant.now(), UUID.randomUUID().toString());
    }
}
