package com.jk.explore.security.stepca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The one-time token step-ca hands out, opened up. It is a JWT: three base64url parts, header.payload.signature. The payload says which
 * names it is for (sub, sans), who made it (iss, the provisioner), which CA may accept it (aud), when it ends (exp) and carries a random id
 * (jti) the CA remembers, so a second use is refused. No tool is needed to read it, so this class is unit-tested on its own.
 */
record Token(int parts, JsonNode header, JsonNode payload) {

    private static final ObjectMapper JSON = new ObjectMapper();

    static Token parse(String token) {
        String[] parts = token.strip().split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        JsonNode header = JSON.readTree(new String(decoder.decode(parts[0]), StandardCharsets.UTF_8));
        JsonNode payload = JSON.readTree(
                new String(decoder.decode(parts[1]), StandardCharsets.UTF_8));
        return new Token(parts.length, header, payload);
    }

    String text(String field) {
        return payload.path(field).asString();
    }

    List<String> sans() {
        List<String> names = new ArrayList<>();
        payload.path("sans").forEach(n -> names.add(n.asString()));
        return names;
    }

    /** How long the token may be used: exp minus nbf (or iat), in seconds. */
    long lifetimeSeconds() {
        long start = payload.has("nbf") ? payload.path("nbf").asLong() : payload.path("iat").asLong();
        return payload.path("exp").asLong() - start;
    }

    /** True when the token's sha claim is the SHA-256 of the root certificate: it names the one root its CA must chain to. */
    boolean pinsRoot(X509Certificate root) {
        return text("sha").equalsIgnoreCase(sha256(root));
    }

    static String sha256(X509Certificate certificate) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
