package com.jk.explore.security.asymmetricbc;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.jcajce.JcaAlgorithmParametersConverter;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;

/**
 * Hybrid encryption in a standard file format: CMS EnvelopedData (RFC 5652), the format behind S/MIME email. It holds the data
 * encrypted with AES-256, and the AES key locked with the recipient's RSA public key, with the recipient named by its certificate.
 * Because it is a standard, other programs, such as OpenSSL, can open it.
 */
public final class CmsEnvelope {

    private CmsEnvelope() {
    }

    /** Seals for one recipient, with RSA-OAEP (SHA-256) for the key. */
    public static byte[] seal(X509Certificate recipient, byte[] data) {
        return seal(recipient, data, true);
    }

    /**
     * Seals for one recipient. With {@code oaep} false, it makes the simple call that most examples show, which picks the
     * older PKCS#1 v1.5 padding for the key: this exists to show what the easy call does.
     */
    public static byte[] seal(X509Certificate recipient, byte[] data, boolean oaep) {
        Providers.ensure();
        try {
            JceKeyTransRecipientInfoGenerator info;
            if (oaep) {
                AlgorithmIdentifier oaepAlgorithm = new JcaAlgorithmParametersConverter().getAlgorithmIdentifier(
                        PKCSObjectIdentifiers.id_RSAES_OAEP,
                        new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
                info = new JceKeyTransRecipientInfoGenerator(recipient, oaepAlgorithm).setProvider(Providers.BC);
            } else {
                info = new JceKeyTransRecipientInfoGenerator(recipient).setProvider(Providers.BC);
            }
            CMSEnvelopedDataGenerator generator = new CMSEnvelopedDataGenerator();
            generator.addRecipientInfoGenerator(info);
            CMSEnvelopedData envelope = generator.generate(new CMSProcessableByteArray(data),
                    new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider(Providers.BC).build());
            return envelope.getEncoded();
        } catch (CMSException | IOException | java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens an envelope with the recipient's private key, or throws {@link NotForThisKeyException}. */
    public static byte[] open(PrivateKey key, X509Certificate recipient, byte[] envelope) {
        Providers.ensure();
        try {
            CMSEnvelopedData data = new CMSEnvelopedData(envelope);
            RecipientId id = new JceKeyTransRecipientId(recipient);
            RecipientInformation info = data.getRecipientInfos().get(id);
            if (info == null) {
                throw new NotForThisKeyException("the envelope is not addressed to this certificate");
            }
            return info.getContent(new JceKeyTransEnvelopedRecipient(key).setProvider(Providers.BC));
        } catch (CMSException e) {
            throw new NotForThisKeyException("this private key cannot open the envelope");
        }
    }

    /** The name of the algorithm used to lock the data key, as the envelope records it, such as an OID. */
    public static String keyTransportAlgorithm(byte[] envelope) {
        try {
            CMSEnvelopedData data = new CMSEnvelopedData(envelope);
            RecipientInformation info = data.getRecipientInfos().getRecipients().iterator().next();
            return info.getKeyEncryptionAlgOID();
        } catch (CMSException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Thrown when a key does not open an envelope. */
    public static class NotForThisKeyException extends RuntimeException {
        public NotForThisKeyException(String message) {
            super(message);
        }
    }
}
