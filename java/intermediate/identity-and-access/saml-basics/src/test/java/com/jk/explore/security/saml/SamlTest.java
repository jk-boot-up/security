package com.jk.explore.security.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SamlTest {

    private static Idp idp;
    private static KeyPair attacker;
    private static final Instant NOW = SamlDemo.NOW;
    private static final String REQ = SamlDemo.REQUEST;

    @BeforeAll
    static void keys() {
        idp = new Idp();
        attacker = Idp.newKey();
    }

    private static String refusal(ServiceProvider sp, String xml) {
        return assertThrows(SamlRefused.class, () -> sp.login(xml, REQ)).getMessage();
    }

    @Test
    void everyProviderAcceptsAGenuineResponse() {
        String r = idp.response(REQ, "ana@bank.example", NOW);
        assertEquals("ana@bank.example", new TrustingServiceProvider().login(r, REQ));
        assertEquals("ana@bank.example", new PinnedKeyServiceProvider(idp.publicKey()).login(r, REQ));
        assertEquals("ana@bank.example", SamlDemo.strict(idp).login(r, REQ));
    }

    @Test
    void theTrustingProviderBelievesAnyKeyInTheSignature() {
        String selfSigned = Idp.signedBy(attacker, REQ, "admin@bank.example", NOW);
        assertEquals("admin@bank.example", new TrustingServiceProvider().login(selfSigned, REQ));
        assertEquals("signature is not valid", refusal(new PinnedKeyServiceProvider(idp.publicKey()), selfSigned));
        assertEquals("signature is not valid", refusal(SamlDemo.strict(idp), selfSigned));
    }

    @Test
    void anEditedAssertionBreaksTheSignature() {
        String edited = Attacks.tamper(idp.response(REQ, "ana@bank.example", NOW), "admin@bank.example");
        assertEquals("signature is not valid", refusal(new TrustingServiceProvider(), edited));
        assertEquals("signature is not valid", refusal(SamlDemo.strict(idp), edited));
    }

    @Test
    void signatureWrappingFoolsAProviderThatReadsADifferentElementThanItChecked() {
        String wrapped = Attacks.wrap(idp.response(REQ, "ana@bank.example", NOW), "admin@bank.example");
        assertEquals("admin@bank.example", new PinnedKeyServiceProvider(idp.publicKey()).login(wrapped, REQ));
        assertEquals("exactly one assertion is required", refusal(SamlDemo.strict(idp), wrapped));
    }

    @Test
    void aResponseUsedTwiceIsRefusedTheSecondTime() {
        StrictServiceProvider sp = SamlDemo.strict(idp);
        String r = idp.response(REQ, "ana@bank.example", NOW);
        sp.login(r, REQ);
        assertEquals("assertion already used", refusal(sp, r));
    }

    @Test
    void timeAudienceAndRequestAreChecked() {
        assertEquals("assertion is outside its validity period", refusal(SamlDemo.strict(idp),
                idp.responseWith(REQ, "a@b", NOW.minusSeconds(3600), StrictServiceProvider.AUDIENCE, NOW.minusSeconds(3000))));
        assertEquals("assertion is outside its validity period", refusal(SamlDemo.strict(idp),
                idp.responseWith(REQ, "a@b", NOW.plusSeconds(3600), StrictServiceProvider.AUDIENCE, NOW.plusSeconds(4000))));
        assertEquals("assertion is for another audience", refusal(SamlDemo.strict(idp), idp.responseWith(REQ, "a@b", NOW, "https://other.example", NOW.plusSeconds(300))));
        assertEquals("assertion answers another request", refusal(SamlDemo.strict(idp), idp.response("req-other", "a@b", NOW)));
    }

    @Test
    void documentsWithoutOrWithOddSignaturesAreRefused() {
        assertEquals("no signature", refusal(new TrustingServiceProvider(), "<a/>"));
        assertEquals("not well-formed XML or has a DOCTYPE", refusal(SamlDemo.strict(idp), "<!DOCTYPE a [<!ENTITY x \"y\">]><a>&x;</a>"));
        assertEquals("not well-formed XML or has a DOCTYPE", refusal(SamlDemo.strict(idp), "<a>"));
        assertEquals("exactly one assertion is required", refusal(SamlDemo.strict(idp), "<a/>"));
    }

    @Test
    void anAssertionThatIsNotSignedAtAllIsRefusedByTheStrictProvider() {
        String r = idp.response(REQ, "ana@bank.example", NOW);
        org.w3c.dom.Document d = Xml.parse(r);
        org.w3c.dom.Node sig = d.getElementsByTagNameNS(Xml.DS, "Signature").item(0);
        sig.getParentNode().removeChild(sig);
        assertEquals("the assertion must carry its own single signature", refusal(SamlDemo.strict(idp), Xml.write(d)));
    }

    @Test
    void theSignatureHasToBeInsideTheAssertionItCovers() {
        String r = idp.response(REQ, "ana@bank.example", NOW);
        org.w3c.dom.Document d = Xml.parse(r);
        org.w3c.dom.Node sig = d.getElementsByTagNameNS(Xml.DS, "Signature").item(0);
        sig.getParentNode().removeChild(sig);
        d.getDocumentElement().appendChild(sig);
        assertEquals("the assertion must carry its own single signature", refusal(SamlDemo.strict(idp), Xml.write(d)));
    }
}
