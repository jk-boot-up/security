package com.jk.explore.security.authkeycloak;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The signature check done by hand must always agree with the library's own RS256 check. */
class SignatureStepsTest {

    private final Issuer issuer = new Issuer(Issuer.REALM_ISSUER, new SimClock(Instant.parse("2026-01-01T09:00:00Z")));

    private static boolean library(String token, Issuer issuer) throws Exception {
        return SignedJWT.parse(token).verify(new RSASSAVerifier(issuer.publicKey()));
    }

    @Test
    void aGoodTokenMatchesByHandAndInTheLibrary() throws Exception {
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        SignatureSteps.Steps steps = SignatureSteps.check(token, issuer.publicKey());
        assertTrue(steps.matches());
        assertTrue(library(token, issuer));
        assertArrayEquals(steps.hash(), steps.hashInSignature());
        assertEquals(202, steps.paddingBytes());
    }

    @Test
    void theHashIsSha256OfHeaderDotClaims() throws Exception {
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        SignatureSteps.Steps steps = SignatureSteps.check(token, issuer.publicKey());
        String signed = token.substring(0, token.lastIndexOf('.'));
        assertEquals(signed, steps.signedText());
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(signed.getBytes(StandardCharsets.US_ASCII)), steps.hash());
    }

    @Test
    void aTamperedTokenFailsByHandAndInTheLibrary() throws Exception {
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        String changed = KeycloakDemo.tamper(token, "\"customer\"", "\"admin\"");
        SignatureSteps.Steps steps = SignatureSteps.check(changed, issuer.publicKey());
        assertFalse(steps.matches());
        assertFalse(library(changed, issuer));
        assertArrayEquals(SignatureSteps.check(token, issuer.publicKey()).hash(), steps.hashInSignature());
    }

    @Test
    void anotherKeyFailsByHandAndInTheLibrary() throws Exception {
        String forged = issuer.forge("ana", List.of("admin"));
        assertFalse(SignatureSteps.check(forged, issuer.publicKey()).matches());
        assertFalse(library(forged, issuer));
    }

    @Test
    void hexIsGroupedInFourBytes() {
        byte[] bytes = {0x05, 0x46, (byte) 0xa3, (byte) 0x81, 0x46, 0x47};
        assertEquals("0546a381 4647", SignatureSteps.hex(bytes, 0, 6));
    }
}
