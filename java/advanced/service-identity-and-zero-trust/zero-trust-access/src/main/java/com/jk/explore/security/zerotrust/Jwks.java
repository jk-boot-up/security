package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Reads the RSA signing keys out of an issuer's JWKS document (RFC 7517). */
final class Jwks {

    private Jwks() {
    }

    static Map<String, PublicKey> parse(String json) throws IOException, GeneralSecurityException {
        Map<String, PublicKey> keys = new HashMap<>();
        KeyFactory rsa = KeyFactory.getInstance("RSA");
        for (JsonNode k : new ObjectMapper().readTree(json).path("keys")) {
            if ("RSA".equals(k.path("kty").asText()) && "sig".equals(k.path("use").asText())) {
                BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(k.path("n").asText()));
                BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(k.path("e").asText()));
                keys.put(k.path("kid").asText(), rsa.generatePublic(new RSAPublicKeySpec(n, e)));
            }
        }
        return keys;
    }
}
