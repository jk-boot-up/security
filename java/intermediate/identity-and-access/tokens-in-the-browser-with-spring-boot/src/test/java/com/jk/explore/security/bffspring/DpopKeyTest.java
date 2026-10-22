package com.jk.explore.security.bffspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** The proofs the app signs, checked without a server. */
class DpopKeyTest {

    @Test
    void aProofNamesTheMethodTheAddressAndTheTime() throws Exception {
        DpopKey key = new DpopKey();
        Instant now = Instant.parse("2026-10-22T10:00:00Z");
        SignedJWT proof = SignedJWT.parse(key.proof("GET", "http://127.0.0.1:1/accounts", null, now, "id-1"));
        assertEquals("dpop+jwt", proof.getHeader().getType().getType());
        assertEquals("ES256", proof.getHeader().getAlgorithm().getName());
        assertEquals("GET", proof.getJWTClaimsSet().getStringClaim("htm"));
        assertEquals("http://127.0.0.1:1/accounts", proof.getJWTClaimsSet().getStringClaim("htu"));
        assertEquals("id-1", proof.getJWTClaimsSet().getJWTID());
        assertEquals(now, proof.getJWTClaimsSet().getIssueTime().toInstant());
        assertNull(proof.getJWTClaimsSet().getClaim("ath"));
    }

    @Test
    void aProofForATokenCarriesTheTokensHash() throws Exception {
        DpopKey key = new DpopKey();
        SignedJWT proof = SignedJWT.parse(key.proof("GET", "http://127.0.0.1:1/accounts", "the-token"));
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest("the-token".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(expected, proof.getJWTClaimsSet().getStringClaim("ath"));
    }

    @Test
    void theHeaderCarriesOnlyThePublicKeyAndTheSignatureVerifiesWithIt() throws Exception {
        DpopKey key = new DpopKey();
        SignedJWT proof = SignedJWT.parse(key.proof("POST", "http://127.0.0.1:1/transfers", "t"));
        ECKey inHeader = (ECKey) proof.getHeader().getJWK();
        assertFalse(inHeader.isPrivate());
        assertTrue(proof.verify(new ECDSAVerifier(inHeader)));
        assertEquals(key.thumbprint(), inHeader.computeThumbprint().toString());
    }

    @Test
    void everyProofHasItsOwnIdAndEveryKeyItsOwnThumbprint() {
        DpopKey a = new DpopKey();
        DpopKey b = new DpopKey();
        assertNotEquals(a.thumbprint(), b.thumbprint());
        assertNotEquals(a.proof("GET", "http://x/", null), a.proof("GET", "http://x/", null));
    }
}
