package com.jk.explore.security.tokens;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Map;

/** A P-256 public key as a JSON Web Key, and its RFC 7638 thumbprint, which is how an access token names the one key it is bound to. */
final class Jwk {

    private Jwk() {
    }

    static String json(ECPublicKey k) {
        return "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"" + B64.encode(fixed(k.getW().getAffineX())) + "\",\"y\":\"" + B64.encode(fixed(k.getW().getAffineY())) + "\"}";
    }

    /** The thumbprint: SHA-256 of the key's members in alphabetical order, with no spaces. */
    static String thumbprint(String x, String y) {
        String canonical = "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"" + x + "\",\"y\":\"" + y + "\"}";
        try {
            return B64.encode(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String thumbprint(ECPublicKey k) {
        return thumbprint(B64.encode(fixed(k.getW().getAffineX())), B64.encode(fixed(k.getW().getAffineY())));
    }

    /** Builds the public key from a parsed JWK, refusing anything that is not a plain P-256 public key. */
    static PublicKey publicKey(Map<String, Object> jwk) {
        try {
            if (!"EC".equals(jwk.get("kty")) || !"P-256".equals(jwk.get("crv")) || jwk.containsKey("d")) {
                throw new IllegalArgumentException("not a P-256 public key");
            }
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);
            ECPoint w = new ECPoint(new BigInteger(1, B64.decode((String) jwk.get("x"))), new BigInteger(1, B64.decode((String) jwk.get("y"))));
            return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, spec));
        } catch (java.security.GeneralSecurityException | RuntimeException e) {
            throw new IllegalArgumentException("bad key");
        }
    }

    private static byte[] fixed(BigInteger n) {
        byte[] raw = n.toByteArray();
        byte[] out = new byte[32];
        if (raw.length > 32) {
            raw = Arrays.copyOfRange(raw, raw.length - 32, raw.length);
        }
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
    }
}
