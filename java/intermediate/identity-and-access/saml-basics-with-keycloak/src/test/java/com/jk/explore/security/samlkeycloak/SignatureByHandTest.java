package com.jk.explore.security.samlkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** The digest and the RSA check redone by hand agree with the JDK's XML signature validation. No server needed. */
class SignatureByHandTest {

    private static final String RESPONSE = "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"r1\">"
            + "<saml:Issuer>bank</saml:Issuer><saml:Assertion ID=\"a1\"><saml:Issuer>bank</saml:Issuer><saml:Subject><saml:NameID>ana</saml:NameID></saml:Subject></saml:Assertion></samlp:Response>";

    /** A self-signed certificate made with keytool for this test only, wrapped the way Keycloak's metadata wraps its own. */
    private static final String METADATA = "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
            + "<md:IDPSSODescriptor><md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:X509Data><ds:X509Certificate>"
            + "MIICwjCCAaqgAwIBAgIJAJCCJIkI4CT2MA0GCSqGSIb3DQEBDAUAMA8xDTALBgNVBAMTBGJhbmswHhcNMjYxMTE0MT"
            + "E0OTM0WhcNMzYxMTExMTE0OTM0WjAPMQ0wCwYDVQQDEwRiYW5rMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtNWdHaY+AxcLLprS"
            + "/I1aRoUGYdpbstT1QwpsBJcTfm//PiNxIUWSa0kwGfwFbriIBR35b4u/TTdLRkG3YO6TRSel2sEK1ORmA6sdudcTxszqXeoeixYz0Fn8bfsZN6"
            + "/6wQptBjk2d+REiERfkfVVu2AmcsuZ81lsvaWg2xbu2zyxJ4aAgvninAi6SQuctR5UhutzyY4loXETL8Znk+JFyz0LvjT1DF4cFCEXwe6N9NR8"
            + "3cRTnJvg0jwtSx5WWxwVp5gBiCS0+b7Ldly1LaNiYFuXw0kdz5l3kSdLSSpV/jeESJP2FphW4PvKWTAYtzhUAIvW1W/xVwwud5+Bf96RAwIDAQ"
            + "ABoyEwHzAdBgNVHQ4EFgQU2lbz9ZP4cUC62ZSWnPYRh83oXDYwDQYJKoZIhvcNAQEMBQADggEBADQXuZRtFvqksJfUhOjMivoQ1BwK1krAQ5c9"
            + "vKTLnrENpvsdhOeqOBluEo6wSm6rOqc84O9JD8wWg5ml3/YhOZ76UyAP61A/bIqxd7x6ieI1Zb8eQtaXznv87vdJTumTtCizZMKwL0njlPeTM7"
            + "nT0mX8thaxSkWwLZ2l1yTKJwILwcY+wa+Ks66TkW8pMIpsIpymTtcAR1ZFKcmBHggKTbeLuBKy4swa51sESnw81aZ1yUQjNrtyElzi8tCN1srM"
            + "hq94GEA4JwU7TnML8rtL2XRxl8PouJ6GzF2D/hBRBNELqkYpvJrIITWg6kWjZGcRA0asWzpsT4dk7wxouplsx9A="
            + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor></md:IDPSSODescriptor></md:EntityDescriptor>";

    private static Document signed(KeyPair key) {
        Document doc = SamlTools.parse(RESPONSE);
        SamlTools.sign(SamlTools.firstAssertion(doc), key);
        SamlTools.sign(SamlTools.response(doc), key);
        return SamlTools.parse(SamlTools.serialize(doc));
    }

    @Test
    void bothSignaturesCheckOutByHandWithTheSigningKey() {
        KeyPair bank = SamlTools.attackerKey();
        List<SignatureByHand.Check> checks = SignatureByHand.checkAll(signed(bank), bank.getPublic());
        assertEquals(List.of("Response", "Assertion"), checks.stream().map(SignatureByHand.Check::covers).toList());
        for (SignatureByHand.Check c : checks) {
            assertTrue(c.digestMatches());
            assertTrue(c.rsaVerifies());
            assertTrue(c.library());
        }
    }

    @Test
    void anotherKeyFailsRsaByHandAndInTheLibraryButTheDigestsStillMatch() {
        Document doc = signed(SamlTools.attackerKey());
        for (SignatureByHand.Check c : SignatureByHand.checkAll(doc, SamlTools.attackerKey().getPublic())) {
            assertTrue(c.digestMatches());
            assertFalse(c.rsaVerifies());
            assertFalse(c.library());
        }
    }

    @Test
    void anEditedNameBreaksBothDigests() {
        KeyPair bank = SamlTools.attackerKey();
        Document doc = signed(bank);
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        for (SignatureByHand.Check c : SignatureByHand.checkAll(SamlTools.parse(SamlTools.serialize(doc)), bank.getPublic())) {
            assertFalse(c.digestMatches());
            assertTrue(c.rsaVerifies()); // SignedInfo itself was not touched
            assertFalse(c.library());
        }
    }

    @Test
    void theKeyIsReadFromTheCertificateInTheMetadata() {
        PublicKey key = SignatureByHand.keyFromMetadata(METADATA);
        assertEquals("RSA", key.getAlgorithm());
        assertEquals(2048, ((RSAPublicKey) key).getModulus().bitLength());
    }
}
