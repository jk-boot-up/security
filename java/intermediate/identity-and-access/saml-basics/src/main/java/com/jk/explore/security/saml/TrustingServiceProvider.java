package com.jk.explore.security.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** The first version: any valid signature will do, with the key it carries, and then the first assertion in the file is the one believed. */
public final class TrustingServiceProvider implements ServiceProvider {

    @Override
    public String login(String responseXml, String pendingRequestId) {
        Document doc = Xml.parse(responseXml);
        Xml.registerIds(doc);
        var sigs = SignatureCheck.signatures(doc);
        if (sigs.isEmpty()) {
            throw new SamlRefused("no signature");
        }
        SignatureCheck.validate(doc, (Element) sigs.get(0), SignatureCheck.EMBEDDED_KEY); // valid for whatever key it names
        Element assertion = (Element) Xml.byName(doc, Xml.SAML, "Assertion").item(0);
        return Xml.text(assertion, Xml.SAML, "NameID");
    }
}
