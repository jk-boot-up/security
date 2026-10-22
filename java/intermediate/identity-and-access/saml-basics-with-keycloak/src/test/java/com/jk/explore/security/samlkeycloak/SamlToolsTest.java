package com.jk.explore.security.samlkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** The attacker's tools, checked without a server. */
class SamlToolsTest {

    private static final String RESPONSE = "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"r1\">"
            + "<saml:Issuer>bank</saml:Issuer><saml:Assertion ID=\"a1\"><saml:Issuer>bank</saml:Issuer><saml:Subject><saml:NameID>ana</saml:NameID></saml:Subject></saml:Assertion></samlp:Response>";

    @Test
    void anAssertionCanBeReadAndEdited() {
        Document doc = SamlTools.parse(RESPONSE);
        assertEquals("ana", SamlTools.nameId(doc));
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        assertEquals("admin", SamlTools.nameId(SamlTools.parse(SamlTools.serialize(doc))));
    }

    @Test
    void signingAddsASignatureAfterTheIssuerAndStrippingRemovesIt() {
        Document doc = SamlTools.parse(RESPONSE);
        KeyPair key = SamlTools.attackerKey();
        SamlTools.sign(SamlTools.firstAssertion(doc), key);
        SamlTools.sign(SamlTools.response(doc), key);
        assertEquals("2 signatures", SamlTools.describeSignatures(doc));
        Document reparsed = SamlTools.parse(SamlTools.serialize(doc));
        assertEquals("2 signatures", SamlTools.describeSignatures(reparsed));
        SamlTools.stripOwnSignature(SamlTools.response(reparsed));
        assertEquals("1 signatures", SamlTools.describeSignatures(reparsed));
        SamlTools.stripSignatures(reparsed);
        assertEquals("0 signatures", SamlTools.describeSignatures(reparsed));
    }

    @Test
    void anUnsignedCopyOfAnAssertionKeepsTheContentButNotTheSignature() {
        Document doc = SamlTools.parse(RESPONSE);
        SamlTools.sign(SamlTools.firstAssertion(doc), SamlTools.attackerKey());
        assertTrue(SamlTools.serialize(SamlTools.firstAssertion(doc).getOwnerDocument()).contains("SignatureValue"));
        var copy = SamlTools.unsignedCopyOfAssertion(doc);
        assertEquals(0, copy.getElementsByTagNameNS(SamlTools.DSIG, "Signature").getLength());
        assertEquals("ana", copy.getElementsByTagNameNS(SamlTools.SAML, "NameID").item(0).getTextContent());
    }

    @Test
    void theAttackersParserRefusesADoctype() {
        assertThrows(IllegalStateException.class, () -> SamlTools.parse("<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x \"y\">]><r>&x;</r>"));
    }

    @Test
    void encodingRoundTrips() {
        assertEquals(RESPONSE, SamlTools.decode(SamlTools.encode(RESPONSE)));
    }
}
