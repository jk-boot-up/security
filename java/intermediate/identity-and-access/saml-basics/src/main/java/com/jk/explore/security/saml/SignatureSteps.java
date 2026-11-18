package com.jk.explore.security.saml;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A teaching class: it redoes by hand the two checks that {@code XMLSignature.validate} hides, so the demo can print each value. The library only
 * canonicalizes; the SHA-256 digest and the RSA verification are done here with the plain JDK, and a test proves they agree with the library.
 */
final class SignatureSteps {

    /** Every value of one check: the canonical bytes, the digests, and the two verdicts by hand next to the library's. */
    record Steps(String coveredId, byte[] canonicalAssertion, String digestValue, String digestByHand, byte[] canonicalSignedInfo,
            int signatureBytes, boolean signatureByHand, boolean library) {

        boolean digestMatches() {
            return digestValue.equals(digestByHand);
        }
    }

    private SignatureSteps() {
    }

    static Steps of(String responseXml, PublicKey key) {
        try {
            Document doc = Xml.parse(responseXml);
            Xml.registerIds(doc);
            Element signatureElement = (Element) SignatureCheck.signatures(doc).get(0);
            DOMValidateContext ctx = new DOMValidateContext(KeySelector.singletonKeySelector(key), signatureElement);
            ctx.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE); // keep the canonical bytes
            XMLSignature sig = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx);
            boolean library = sig.validate(ctx);
            Reference ref = (Reference) sig.getSignedInfo().getReferences().get(0);
            ref.validate(ctx); // computes the digest even when the signature value already failed

            // 1. the assertion without its Signature, canonicalized: the exact bytes that are hashed
            byte[] canonical = readAll(ref.getDigestInputStream());
            // 2. DigestValue = base64(SHA-256(those bytes))
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String byHand = Base64.getEncoder().encodeToString(sha256.digest(canonical));
            String declared = Base64.getEncoder().encodeToString(ref.getDigestValue());
            // 3. SignatureValue = RSA-SHA256 over the canonical SignedInfo, which holds that DigestValue
            byte[] signedInfo = readAll(sig.getSignedInfo().getCanonicalizedData());
            byte[] value = sig.getSignatureValue().getValue();
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(key);
            rsa.update(signedInfo);
            boolean signatureByHand = rsa.verify(value);
            return new Steps(ref.getURI().substring(1), canonical, declared, byHand, signedInfo, value.length, signatureByHand, library);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        try (in) {
            return in.readAllBytes();
        }
    }
}
