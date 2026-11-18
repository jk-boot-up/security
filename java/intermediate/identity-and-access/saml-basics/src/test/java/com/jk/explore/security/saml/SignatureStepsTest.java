package com.jk.explore.security.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

/** The digest and the RSA check redone by hand give exactly the library's verdict, and the digest does not depend on the key. */
class SignatureStepsTest {

    private static final String REQ = SamlDemo.REQUEST;

    @Test
    void byHandAgreesWithTheLibraryForAGenuineResponse() {
        Idp idp = new Idp();
        SignatureSteps.Steps s = SignatureSteps.of(idp.response(REQ, "ana@bank.example", SamlDemo.NOW), idp.publicKey());
        assertEquals("a1", s.coveredId());
        assertTrue(s.digestMatches());
        assertTrue(s.signatureByHand());
        assertTrue(s.library());
        assertEquals(256, s.signatureBytes());
    }

    @Test
    void theDigestIsTheSameWhicheverKeySigns() {
        Idp one = new Idp();
        Idp two = new Idp();
        String d1 = SignatureSteps.of(one.response(REQ, "ana@bank.example", SamlDemo.NOW), one.publicKey()).digestValue();
        String d2 = SignatureSteps.of(two.response(REQ, "ana@bank.example", SamlDemo.NOW), two.publicKey()).digestValue();
        assertEquals(d1, d2);
    }

    @Test
    void anEditChangesTheDigestAndTheLibraryRefuses() {
        Idp idp = new Idp();
        String edited = Attacks.tamper(idp.response(REQ, "ana@bank.example", SamlDemo.NOW), "admin@bank.example");
        SignatureSteps.Steps s = SignatureSteps.of(edited, idp.publicKey());
        assertNotEquals(s.digestValue(), s.digestByHand());
        assertTrue(s.signatureByHand()); // SignedInfo itself was not touched
        assertFalse(s.library());
    }

    @Test
    void anotherKeyFailsTheRsaCheckByHandAndInTheLibrary() {
        Idp idp = new Idp();
        KeyPair other = Idp.newKey();
        SignatureSteps.Steps s = SignatureSteps.of(idp.response(REQ, "ana@bank.example", SamlDemo.NOW), other.getPublic());
        assertTrue(s.digestMatches());
        assertFalse(s.signatureByHand());
        assertFalse(s.library());
    }
}
