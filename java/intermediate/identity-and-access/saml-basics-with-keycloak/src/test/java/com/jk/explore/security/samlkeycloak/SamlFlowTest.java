package com.jk.explore.security.samlkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.w3c.dom.Document;

/** A real Keycloak in Docker and the real shop. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamlFlowTest {

    private KeycloakLab keycloak;
    private Running shop;
    private Refusals refusals;

    @BeforeAll
    void start() throws Exception {
        int port = Running.freePort();
        keycloak = KeycloakLab.start(port);
        shop = Running.start(keycloak, port, "bank");
        refusals = shop.context().getBean(Refusals.class);
    }

    @AfterAll
    void stop() {
        shop.close();
        keycloak.close();
    }

    private String attempt(String user, java.util.function.Consumer<Document> attack) throws Exception {
        SamlKeycloakDemo.Stopped s = SamlKeycloakDemo.signedInAtTheBank(shop, user);
        Document doc = s.response();
        attack.accept(doc);
        return SamlKeycloakDemo.post(shop, s, doc, refusals);
    }

    @Test
    void aGenuineResponseSignsTheShopperIn() throws Exception {
        assertEquals("signed in as ana", attempt("ana", d -> { }));
    }

    @Test
    void theResponseIsSignedAsAWholeAndAsAnAssertion() throws Exception {
        assertEquals("2 signatures", SamlTools.describeSignatures(SamlKeycloakDemo.signedInAtTheBank(shop, "ana").response()));
    }

    @Test
    void theAuthnRequestIsUnsignedAndNamesTheShopAndItsAcs() throws Exception {
        Shopper.Page p = new Shopper().navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Document req = SamlTools.parse(SamlTools.decode(Shopper.hiddenFields(p.body()).get("SAMLRequest")));
        assertEquals(0, req.getElementsByTagNameNS(SamlTools.DSIG, "Signature").getLength());
        assertEquals(shop.url() + Running.ACS_PATH, req.getDocumentElement().getAttribute("AssertionConsumerServiceURL"));
    }

    @Test
    void anEditedNameBreaksTheSignature() throws Exception {
        assertEquals("refused (invalid_signature)", attempt("ana", d -> SamlTools.setNameId(SamlTools.firstAssertion(d), "admin")));
    }

    @Test
    void aSelfSignedResponseIsRefused() throws Exception {
        assertEquals("refused (invalid_signature)", attempt("ana", d -> {
            SamlTools.stripSignatures(d);
            SamlTools.setNameId(SamlTools.firstAssertion(d), "admin");
            var key = SamlTools.attackerKey();
            SamlTools.sign(SamlTools.firstAssertion(d), key);
            SamlTools.sign(SamlTools.response(d), key);
        }));
    }

    @Test
    void anUnsignedResponseIsRefused() throws Exception {
        assertEquals("refused (invalid_signature)", attempt("ana", d -> SamlTools.stripSignatures(d)));
    }

    @Test
    void aSignedAssertionWithAnUnsignedOneBesideItIsRefusedWhicheverComesFirst() throws Exception {
        for (boolean evilFirst : new boolean[] {true, false}) {
            assertEquals("refused (invalid_signature)", attempt("ana", d -> {
                SamlTools.stripOwnSignature(SamlTools.response(d));
                var evil = SamlTools.unsignedCopyOfAssertion(d);
                SamlTools.setNameId(evil, "admin");
                var signed = SamlTools.firstAssertion(d);
                if (evilFirst) {
                    signed.getParentNode().insertBefore(evil, signed);
                } else {
                    signed.getParentNode().appendChild(evil);
                }
            }));
        }
    }

    @Test
    void aDoctypeIsRefusedAsMalformed() throws Exception {
        SamlKeycloakDemo.Stopped s = SamlKeycloakDemo.signedInAtTheBank(shop, "ana");
        assertEquals("refused (malformed_response_data)", SamlKeycloakDemo.post(shop, s.browser(), s.fields(), "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x \"y\">]><r>&x;</r>", refusals));
    }

    @Test
    void aResponseCannotBeReplayedFromAnotherBrowserOrTheSameOne() throws Exception {
        SamlKeycloakDemo.Stopped s = SamlKeycloakDemo.signedInAtTheBank(shop, "ana");
        String xml = SamlTools.decode(s.fields().get("SAMLResponse"));
        assertEquals("signed in as ana", SamlKeycloakDemo.post(shop, s.browser(), s.fields(), xml, refusals));
        assertEquals("refused (invalid_in_response_to)", SamlKeycloakDemo.post(shop, new Shopper(), s.fields(), xml, refusals));
        assertEquals("refused (invalid_in_response_to)", SamlKeycloakDemo.post(shop, s.browser(), s.fields(), xml, refusals));
    }

    @Test
    void aGenuineResponseForAnotherShopIsRefusedForItsAudience() throws Exception {
        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replaceFirst("(<saml2:Issuer[^>]*>)[^<]+", "$1http://other-shop.example/saml/metadata")));
        Map<String, String> fields = Shopper.hiddenFields(new Shopper().navigatePost(keycloak.issuer("bank") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH).body());
        assertTrue(SamlTools.decode(fields.get("SAMLResponse")).contains("other-shop.example"));
        assertEquals("refused (invalid_assertion)", SamlKeycloakDemo.post(shop, b, fields, SamlTools.decode(fields.get("SAMLResponse")), refusals));
    }

    @Test
    void anExpiredAssertionIsRefused() throws Exception {
        SamlKeycloakDemo.Stopped s = SamlKeycloakDemo.signedInAtTheBank(shop, "ana");
        Thread.sleep(4_500);
        assertEquals("refused (invalid_assertion)", SamlKeycloakDemo.post(shop, s.browser(), s.fields(), SamlTools.decode(s.fields().get("SAMLResponse")), refusals));
    }

    @Test
    void aGenuineResponseFromTheWalletIsRefusedBecauseItsKeyIsNotTheBanks() throws Exception {
        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replace(keycloak.issuer("bank") + "/protocol/saml", keycloak.issuer("wallet") + "/protocol/saml")));
        Map<String, String> fields = Shopper.hiddenFields(new Shopper().navigatePost(keycloak.issuer("wallet") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH).body());
        assertEquals("refused (invalid_signature)", SamlKeycloakDemo.post(shop, b, fields, SamlTools.decode(fields.get("SAMLResponse")), refusals));
    }
}
