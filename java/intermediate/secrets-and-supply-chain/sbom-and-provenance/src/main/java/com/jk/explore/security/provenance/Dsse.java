package com.jk.explore.security.provenance;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Dead Simple Signing Envelope (DSSE), the envelope in-toto uses. The signature covers the pre-authentication encoding (PAE) of the
 * payload type and the payload, so a signature over one type of document cannot be replayed as another.
 */
final class Dsse {

    static final String PAYLOAD_TYPE = "application/vnd.in-toto+json";

    private Dsse() {
    }

    /** PAE(type, body) = "DSSEv1" SP LEN(type) SP type SP LEN(body) SP body, with lengths in bytes as decimal ASCII. */
    static byte[] pae(String payloadType, byte[] body) {
        byte[] type = payloadType.getBytes(StandardCharsets.UTF_8);
        byte[] head = ("DSSEv1 " + type.length + " " + payloadType + " " + body.length + " ").getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[head.length + body.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(body, 0, out, head.length, body.length);
        return out;
    }

    static String envelope(byte[] payload, String keyId, PrivateKey key) {
        byte[] signature = Ed25519.sign(key, pae(PAYLOAD_TYPE, payload));
        return Json.write(Json.object(
                "payload", Base64.getEncoder().encodeToString(payload),
                "payloadType", PAYLOAD_TYPE,
                "signatures", List.of(Json.object("keyid", keyId, "sig", Base64.getEncoder().encodeToString(signature)))));
    }

    /** What a verifier gets back: the payload, if one signature with a trusted key verified over its PAE. */
    record Opened(byte[] payload, String keyId) {
    }

    static final class BadEnvelopeException extends Exception {
        BadEnvelopeException(String message) {
            super(message);
        }
    }

    static Opened open(String envelope, Map<String, PublicKey> trusted) throws BadEnvelopeException {
        Object root;
        try {
            root = Json.read(envelope);
        } catch (Json.BadJsonException e) {
            throw new BadEnvelopeException("the envelope is not valid JSON");
        }
        if (!PAYLOAD_TYPE.equals(Json.text(root, "payloadType"))) {
            throw new BadEnvelopeException("the payload is not an in-toto statement");
        }
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(String.valueOf(Json.text(root, "payload")));
        } catch (IllegalArgumentException e) {
            throw new BadEnvelopeException("the payload is not base64");
        }
        if (!(Json.at(root, "signatures") instanceof List<?> signatures) || signatures.isEmpty()) {
            throw new BadEnvelopeException("the envelope has no signature");
        }
        boolean knownKey = false;
        for (Object entry : signatures) {
            String keyId = Json.text(entry, "keyid");
            PublicKey key = keyId == null ? null : trusted.get(keyId);
            if (key == null) {
                continue;
            }
            knownKey = true;
            byte[] signature;
            try {
                signature = Base64.getDecoder().decode(String.valueOf(Json.text(entry, "sig")));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (Ed25519.verify(key, pae(PAYLOAD_TYPE, payload), signature)) {
                return new Opened(payload, keyId);
            }
        }
        throw new BadEnvelopeException(knownKey ? "the signature does not match the statement" : "no signature from a trusted builder");
    }
}
