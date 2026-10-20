package com.jk.explore.security.saml;

import java.security.PublicKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The second version: it trusts only the identity provider's own key, which fixes the self-signed response. But it still checks "some signature is valid" and then
 * reads "the first assertion", which are two different questions.
 */
public final class PinnedKeyServiceProvider implements ServiceProvider {

    private final PublicKey idpKey;

    public PinnedKeyServiceProvider(PublicKey idpKey) {
        this.idpKey = idpKey;
    }

    @Override
    public String login(String responseXml, String pendingRequestId) {
        Document doc = Xml.parse(responseXml);
        Xml.registerIds(doc);
        var sigs = SignatureCheck.signatures(doc);
        if (sigs.isEmpty()) {
            throw new SamlRefused("no signature");
        }
        SignatureCheck.validate(doc, (Element) sigs.get(0), SignatureCheck.pinned(idpKey));
        Element assertion = (Element) Xml.byName(doc, Xml.SAML, "Assertion").item(0); // the FIRST assertion, signed or not
        return Xml.text(assertion, Xml.SAML, "NameID");
    }
}
