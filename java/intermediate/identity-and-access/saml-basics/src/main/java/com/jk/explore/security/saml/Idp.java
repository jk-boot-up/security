package com.jk.explore.security.saml;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An identity provider: it signs the <em>assertion</em> (the statement "this is ana@bank.example") with its private key. Anyone can build the same XML
 * with a different key, which is why a service provider must know which key to trust.
 */
public final class Idp {

    public static final String ISSUER = "https://idp.bank.example";

    private final KeyPair key;
    private static int counter;

    public Idp() {
        this(newKey());
    }

    public Idp(KeyPair key) {
        this.key = key;
    }

    public static KeyPair newKey() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public PublicKey publicKey() {
        return key.getPublic();
    }

    /** A signed response for a sign-in that this service provider requested. */
    public String response(String inResponseTo, String nameId, Instant now) {
        return sign(unsignedResponse(inResponseTo, nameId, now, "https://shop.example", now.plusSeconds(300)), key);
    }

    /** The same XML shape, signed by whichever key the caller holds: an attacker can do this too. */
    public static String signedBy(KeyPair signer, String inResponseTo, String nameId, Instant now) {
        return sign(unsignedResponse(inResponseTo, nameId, now, "https://shop.example", now.plusSeconds(300)), signer);
    }

    public String responseWith(String inResponseTo, String nameId, Instant now, String audience, Instant notOnOrAfter) {
        return sign(unsignedResponse(inResponseTo, nameId, now, audience, notOnOrAfter), key);
    }

    private static String assertionId() {
        return "a" + (++counter) + "-" + Long.toHexString(System.nanoTime());
    }

    private static Document unsignedResponse(String inResponseTo, String nameId, Instant now, String audience, Instant notOnOrAfter) {
        String id = assertionId();
        String xml = "<samlp:Response xmlns:samlp=\"" + Xml.SAMLP + "\" xmlns:saml=\"" + Xml.SAML + "\" ID=\"r-" + id + "\" InResponseTo=\"" + inResponseTo
                + "\" Destination=\"https://shop.example/acs\">"
                + "<saml:Assertion ID=\"" + id + "\" IssueInstant=\"" + now + "\">"
                + "<saml:Issuer>" + ISSUER + "</saml:Issuer>"
                + "<saml:Subject><saml:NameID>" + nameId + "</saml:NameID>"
                + "<saml:SubjectConfirmationData InResponseTo=\"" + inResponseTo + "\" Recipient=\"https://shop.example/acs\" NotOnOrAfter=\"" + notOnOrAfter + "\"/></saml:Subject>"
                + "<saml:Conditions NotBefore=\"" + now.minusSeconds(60) + "\" NotOnOrAfter=\"" + notOnOrAfter + "\">"
                + "<saml:AudienceRestriction><saml:Audience>" + audience + "</saml:Audience></saml:AudienceRestriction></saml:Conditions>"
                + "</saml:Assertion></samlp:Response>";
        return Xml.parse(xml);
    }

    /** Signs the Assertion inside the document with an enveloped signature, and puts the public key in KeyInfo (as many identity providers do). */
    static String sign(Document doc, KeyPair signer) {
        try {
            Xml.registerIds(doc);
            Element assertion = (Element) Xml.byName(doc, Xml.SAML, "Assertion").item(0);
            XMLSignatureFactory f = XMLSignatureFactory.getInstance("DOM");
            Reference ref = f.newReference("#" + assertion.getAttribute("ID"), f.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(f.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                            f.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)), null, null);
            SignedInfo si = f.newSignedInfo(f.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    f.newSignatureMethod(SignatureMethod.RSA_SHA256, null), Collections.singletonList(ref));
            KeyInfoFactory kif = f.getKeyInfoFactory();
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kif.newKeyValue(signer.getPublic())));
            DOMSignContext ctx = new DOMSignContext(signer.getPrivate(), assertion, assertion.getElementsByTagNameNS(Xml.SAML, "Subject").item(0));
            f.newXMLSignature(si, ki).sign(ctx);
            return Xml.write(doc);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean hasSignature(Document d) {
        return Xml.byName(d, Xml.DS, "Signature").getLength() > 0;
    }
}
