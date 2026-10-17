package com.jk.explore.security.signaturesbc;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;

/**
 * CMS SignedData (RFC 5652), the standard signed-message format behind S/MIME email, signed PDFs and code signing. It carries
 * the signature, the signer's certificate and signed attributes such as the claimed signing time. It can hold the content
 * (attached) or leave it out (detached), so the content travels separately.
 */
final class Cms {

    private Cms() {
    }

    /** Signs the content. The signing time is passed in, so the demo is repeatable; a real signer uses the clock. */
    static byte[] sign(X509Certificate certificate, PrivateKey key, byte[] content, boolean attached, Instant signingTime) {
        Providers.ensure();
        try {
            ASN1EncodableVector attributes = new ASN1EncodableVector();
            attributes.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(Date.from(signingTime)))));
            var signerInfo = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(Providers.BC)
                    .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(new AttributeTable(attributes)))
                    .build("SHA256withECDSA", key, certificate);
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(signerInfo);
            generator.addCertificates(new JcaCertStore(List.of(certificate)));
            return generator.generate(new CMSProcessableByteArray(content), attached).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Verifies every signer against the given certificate. Detached content is passed in; for attached, pass null. */
    static boolean verify(byte[] signedData, byte[] detachedContent, X509Certificate certificate) {
        Providers.ensure();
        try {
            CMSSignedData data = detachedContent == null
                    ? new CMSSignedData(signedData)
                    : new CMSSignedData(new CMSProcessableByteArray(detachedContent), signedData);
            var signers = new ArrayList<>(data.getSignerInfos().getSigners());
            if (signers.isEmpty()) {
                return false;
            }
            for (SignerInformation signer : signers) {
                if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(Providers.BC).build(certificate))) {
                    return false;
                }
            }
            return true;
        } catch (CMSException | org.bouncycastle.operator.OperatorCreationException e) {
            return false;
        }
    }

    /** The signing time the signer claimed. It is covered by the signature but it is the signer's word, not a trusted clock. */
    static Instant claimedSigningTime(byte[] signedData) {
        try {
            SignerInformation signer = new CMSSignedData(signedData).getSignerInfos().getSigners().iterator().next();
            Attribute attribute = signer.getSignedAttributes().get(CMSAttributes.signingTime);
            return Time.getInstance(attribute.getAttrValues().getObjectAt(0)).getDate().toInstant();
        } catch (CMSException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
