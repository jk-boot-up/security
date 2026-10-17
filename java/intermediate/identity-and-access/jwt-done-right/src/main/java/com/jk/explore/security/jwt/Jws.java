package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;

/** The compact JWS form, header.payload.signature (RFC 7515 section 7.1), with Ed25519 signatures. */
final class Jws {

    /** A token split into its parts. Nothing in it has been verified yet. */
    record Parts(Map<String, Object> header, String headerPart, String payloadPart, byte[] payload, byte[] signature) {

        byte[] signingInput() {
            return (headerPart + "." + payloadPart).getBytes(StandardCharsets.US_ASCII);
        }
    }

    private Jws() {
    }

    static String sign(Map<String, ?> header, byte[] payload, PrivateKey key) {
        String headerPart = Base64Url.encode(Json.write(header).getBytes(StandardCharsets.UTF_8));
        String payloadPart = Base64Url.encode(payload);
        byte[] signature = Ed25519.sign(key, (headerPart + "." + payloadPart).getBytes(StandardCharsets.US_ASCII));
        return headerPart + "." + payloadPart + "." + Base64Url.encode(signature);
    }

    /** Splits and decodes a compact token. Exactly three parts; the header must be a JSON object without duplicate members. */
    static Parts parse(String token) throws InvalidTokenException {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new InvalidTokenException("not three parts");
        }
        byte[] headerBytes = Base64Url.decode(parts[0]);
        Map<String, Object> header = Json.parseObject(new String(headerBytes, StandardCharsets.UTF_8));
        return new Parts(header, parts[0], parts[1], Base64Url.decode(parts[1]), Base64Url.decode(parts[2]));
    }
}
