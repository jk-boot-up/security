package com.jk.explore.security.saml;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The fixed version. The rule that matters: <strong>use only what the signature covers.</strong> The assertion that is read is the very element the validated
 * signature points at, it is the only assertion in the document, and the signature sits inside it. Then the statement itself is checked: issuer, audience,
 * time, the request it answers, where it was sent, and that it has not been used before.
 */
public final class StrictServiceProvider implements ServiceProvider {

    static final String AUDIENCE = "https://shop.example";
    static final String ACS = "https://shop.example/acs";
    private static final long SKEW_SECONDS = 60;

    private final PublicKey idpKey;
    private final Clock clock;
    private final Set<String> usedAssertions = new HashSet<>();

    public StrictServiceProvider(PublicKey idpKey, Clock clock) {
        this.idpKey = idpKey;
        this.clock = clock;
    }

    @Override
    public String login(String responseXml, String pendingRequestId) {
        Document doc = Xml.parse(responseXml);
        Xml.registerIds(doc);

        NodeList assertions = Xml.byName(doc, Xml.SAML, "Assertion");
        if (assertions.getLength() != 1) {
            throw new SamlRefused("exactly one assertion is required");
        }
        Element assertion = (Element) assertions.item(0);
        Element response = doc.getDocumentElement();
        if (assertion.getParentNode() != response || !response.getLocalName().equals("Response")) {
            throw new SamlRefused("assertion is not a direct child of the response");
        }
        var sigs = SignatureCheck.signatures(doc);
        if (sigs.size() != 1 || sigs.get(0).getParentNode() != assertion) {
            throw new SamlRefused("the assertion must carry its own single signature");
        }
        SignatureCheck.Result checked = SignatureCheck.validate(doc, (Element) sigs.get(0), SignatureCheck.pinned(idpKey));
        String id = assertion.getAttribute("ID");
        if (id.isEmpty() || !id.equals(checked.coveredId()) || countIds(doc, id) != 1) {
            throw new SamlRefused("the signature does not cover the assertion that was read");
        }

        if (!Idp.ISSUER.equals(Xml.text(assertion, Xml.SAML, "Issuer"))) {
            throw new SamlRefused("wrong issuer");
        }
        Element conditions = (Element) assertion.getElementsByTagNameNS(Xml.SAML, "Conditions").item(0);
        Element confirmation = (Element) assertion.getElementsByTagNameNS(Xml.SAML, "SubjectConfirmationData").item(0);
        if (conditions == null || confirmation == null) {
            throw new SamlRefused("conditions and subject confirmation are required");
        }
        Instant now = clock.instant();
        if (now.plusSeconds(SKEW_SECONDS).isBefore(Instant.parse(conditions.getAttribute("NotBefore")))
                || !now.minusSeconds(SKEW_SECONDS).isBefore(Instant.parse(conditions.getAttribute("NotOnOrAfter")))) {
            throw new SamlRefused("assertion is outside its validity period");
        }
        if (!AUDIENCE.equals(Xml.text(conditions, Xml.SAML, "Audience"))) {
            throw new SamlRefused("assertion is for another audience");
        }
        if (!ACS.equals(confirmation.getAttribute("Recipient")) || !ACS.equals(response.getAttribute("Destination"))) {
            throw new SamlRefused("assertion was sent to another address");
        }
        if (!pendingRequestId.equals(confirmation.getAttribute("InResponseTo"))) {
            throw new SamlRefused("assertion answers another request");
        }
        if (!usedAssertions.add(id)) {
            throw new SamlRefused("assertion already used");
        }
        return Xml.text(assertion, Xml.SAML, "NameID");
    }

    private static int countIds(Document doc, String id) {
        int n = 0;
        NodeList all = doc.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Node e = all.item(i);
            if (e instanceof Element el && el.hasAttribute("ID") && el.getAttribute("ID").equals(id)) {
                n++;
            }
        }
        return n;
    }
}
