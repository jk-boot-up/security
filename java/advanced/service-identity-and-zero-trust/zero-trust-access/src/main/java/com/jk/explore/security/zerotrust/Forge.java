package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What an attacker (and the tests) can make without Keycloak: tokens built and edited by hand. */
final class Forge {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private Forge() {
    }

    static KeyPair newKey() throws GeneralSecurityException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    /** A token with the given header and claims, signed with the key (or unsigned when the key is null). */
    static String token(Map<String, Object> header, Map<String, Object> claims, KeyPair key) throws Exception {
        String signing = B64.encodeToString(JSON.writeValueAsBytes(header)) + "." + B64.encodeToString(JSON.writeValueAsBytes(claims));
        if (key == null) {
            return signing + ".";
        }
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(key.getPrivate());
        s.update(signing.getBytes(StandardCharsets.US_ASCII));
        return signing + "." + B64.encodeToString(s.sign());
    }

    static Map<String, Object> claims(String issuer, String audience, String user, List<String> roles, boolean compliant, long exp) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("iss", issuer);
        c.put("aud", audience);
        c.put("exp", exp);
        c.put("preferred_username", user);
        c.put("realm_access", Map.of("roles", roles));
        c.put("compliant", compliant);
        return c;
    }

    /** The same token with other claims and the old signature kept. */
    @SuppressWarnings("unchecked")
    static String withEditedClaims(String token, Map<String, Object> edits) throws Exception {
        String[] p = token.split("\\.");
        Map<String, Object> claims = JSON.readValue(Base64.getUrlDecoder().decode(p[1]), Map.class);
        claims.putAll(edits);
        return p[0] + "." + B64.encodeToString(JSON.writeValueAsBytes(claims)) + "." + p[2];
    }

    /** The same token with one character of the signature changed. */
    static String withFlippedSignature(String token) {
        int i = token.lastIndexOf('.') + 1 + 20;
        char c = token.charAt(i);
        return token.substring(0, i) + (c == 'A' ? 'B' : 'A') + token.substring(i + 1);
    }
}
