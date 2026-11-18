package com.jk.explore.security.samlkeycloak;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A teaching class: what OpenSAML does with each signature, redone with the plain JDK so the demo can print it. The JDK canonicalizes; the SHA-256 digest
 * and the RSA verification are done here by hand. The shop itself never uses this class: Spring Security and OpenSAML check the response.
 */
final class SignatureByHand {

    /** One signature: the element it covers, whether its digest matches that element now, and whether RSA verifies with the given key. */
    record Check(String covers, boolean digestMatches, boolean rsaVerifies, boolean library) {
    }

    private SignatureByHand() {
    }

    /** The bank's signing key, as the shop reads it: the X.509 certificate in the bank's SAML metadata. */
    static PublicKey keyFromMetadata(String metadataXml) {
        try {
            Document metadata = SamlTools.parse(metadataXml);
            String base64 = metadata.getElementsByTagNameNS(SamlTools.DSIG, "X509Certificate").item(0).getTextContent();
            byte[] der = Base64.getMimeDecoder().decode(base64);
            return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der)).getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every signature in the document, in document order, checked with {@code key}. */
    static java.util.List<Check> checkAll(Document doc, PublicKey key) {
        registerIds(doc);
        NodeList signatures = doc.getElementsByTagNameNS(SamlTools.DSIG, "Signature");
        java.util.List<Check> checks = new java.util.ArrayList<>();
        for (int i = 0; i < signatures.getLength(); i++) {
            checks.add(check((Element) signatures.item(i), key));
        }
        return checks;
    }

    static Check check(Element signatureElement, PublicKey key) {
        try {
            DOMValidateContext ctx = new DOMValidateContext(KeySelector.singletonKeySelector(key), signatureElement);
            ctx.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE); // keep the canonical bytes
            XMLSignature sig = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx);
            boolean library = sig.validate(ctx);
            Reference ref = (Reference) sig.getSignedInfo().getReferences().get(0);
            ref.validate(ctx);

            // 1. the covered element, canonicalized, hashed again: is it still what was signed?
            byte[] canonical = readAll(ref.getDigestInputStream());
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            boolean digestMatches = MessageDigest.isEqual(digest, ref.getDigestValue());
            // 2. SignedInfo (which holds that digest), verified with RSA and the key the shop trusts
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(key);
            rsa.update(readAll(sig.getSignedInfo().getCanonicalizedData()));
            boolean rsaVerifies = rsa.verify(sig.getSignatureValue().getValue());

            Element covered = signatureElement.getOwnerDocument().getElementById(ref.getURI().substring(1));
            return new Check(covered == null ? "nothing" : covered.getLocalName(), digestMatches, rsaVerifies, library);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void registerIds(Document doc) {
        NodeList all = doc.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            if (e.hasAttribute("ID")) {
                e.setIdAttribute("ID", true);
            }
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        try (in) {
            return in.readAllBytes();
        }
    }
}
