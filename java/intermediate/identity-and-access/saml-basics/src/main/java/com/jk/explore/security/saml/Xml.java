package com.jk.explore.security.saml;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** XML helpers shared by the identity provider and the service providers. Parsing refuses a DOCTYPE (see the xml-and-template-injection project). */
final class Xml {

    static final String SAML = "urn:oasis:names:tc:SAML:2.0:assertion";
    static final String SAMLP = "urn:oasis:names:tc:SAML:2.0:protocol";
    static final String DS = "http://www.w3.org/2000/09/xmldsig#";

    private Xml() {
    }

    static Document parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new SamlRefused("not well-formed XML or has a DOCTYPE");
        }
    }

    static String write(Document d) {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter w = new StringWriter();
            t.transform(new DOMSource(d), new StreamResult(w));
            return w.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Tells the XML signature code which attribute is an ID, so that "#a1" can find the element. */
    static void registerIds(Document d) {
        NodeList all = d.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            if (e.hasAttribute("ID")) {
                e.setIdAttribute("ID", true);
            }
        }
    }

    static NodeList byName(Document d, String ns, String name) {
        return d.getElementsByTagNameNS(ns, name);
    }

    static String text(Element parent, String ns, String name) {
        NodeList n = parent.getElementsByTagNameNS(ns, name);
        return n.getLength() == 0 ? null : n.item(0).getTextContent().trim();
    }
}
