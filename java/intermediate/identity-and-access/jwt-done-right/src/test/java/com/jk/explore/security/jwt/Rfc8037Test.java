package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The Ed25519 code is checked against RFC 8037 appendix A, which publishes a key and a signed JWS. */
class Rfc8037Test {

    @Test
    void thePublishedTokenVerifiesWithThePublishedKey() throws Exception {
        Jws.Parts parts = Jws.parse(JwtDemo.RFC8037_TOKEN);
        assertEquals("{\"alg\":\"EdDSA\"}", new String(Base64Url.decode(parts.headerPart()), StandardCharsets.UTF_8));
        assertEquals("Example of Ed25519 signing", new String(parts.payload(), StandardCharsets.UTF_8));
        assertTrue(Ed25519.verify(Ed25519.publicKey(Base64Url.decode(JwtDemo.RFC8037_X)), parts.signingInput(), parts.signature()));
    }

    @Test
    void signingThePublishedInputGivesThePublishedSignature() throws Exception {
        Jws.Parts parts = Jws.parse(JwtDemo.RFC8037_TOKEN);
        byte[] seed = HexFormat.of().parseHex(JwtDemo.RFC8037_SEED);
        assertArrayEquals(parts.signature(), Ed25519.sign(Ed25519.privateKey(seed), parts.signingInput()));
    }

    @Test
    void thePublicKeyDerivedFromTheSeedIsThePublishedOne() throws Exception {
        byte[] seed = HexFormat.of().parseHex(JwtDemo.RFC8037_SEED);
        assertArrayEquals(Base64Url.decode(JwtDemo.RFC8037_X), Ed25519.encode(Ed25519.fromSeed(seed).getPublic()));
    }

    @Test
    void aChangedPayloadOrSignatureDoesNotVerify() throws Exception {
        Jws.Parts parts = Jws.parse(JwtDemo.RFC8037_TOKEN);
        byte[] input = parts.signingInput();
        input[input.length - 1] ^= 1;
        assertFalse(Ed25519.verify(Ed25519.publicKey(Base64Url.decode(JwtDemo.RFC8037_X)), input, parts.signature()));
        byte[] signature = parts.signature().clone();
        signature[0] ^= 1;
        assertFalse(Ed25519.verify(Ed25519.publicKey(Base64Url.decode(JwtDemo.RFC8037_X)), parts.signingInput(), signature));
        assertFalse(Ed25519.verify(Ed25519.publicKey(Base64Url.decode(JwtDemo.RFC8037_X)), parts.signingInput(), new byte[10]));
    }
}
