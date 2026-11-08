package com.jk.explore.security.asymmetricbc;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EnvelopedData;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.asn1.cms.RecipientInfo;

/**
 * Opens a CMS envelope by hand, step by step, with the JDK's own ciphers: read its parts, unlock the AES key with RSA-OAEP,
 * then decrypt the statement with AES-CBC and remove the padding. Bouncy Castle does all of this inside
 * {@link CmsEnvelope#open}; this class shows the steps, and a test proves it gives the same statement.
 */
public final class EnvelopeSteps {

    private EnvelopeSteps() {
    }

    /** What the envelope holds: who it is for, the locked AES key, the IV and the encrypted statement. */
    public record Parts(BigInteger serial, byte[] lockedKey, byte[] iv, byte[] cipherText) {
    }

    /** What each step of opening gives: the AES key, the decrypted bytes with their padding, and the statement. */
    public record Opened(byte[] dataKey, byte[] padded, byte[] statement) {
    }

    /** Step one: read the parts of the envelope (only reading, no decryption). */
    public static Parts parts(byte[] envelope) {
        try {
            EnvelopedData data = EnvelopedData.getInstance(ContentInfo.getInstance(ASN1Primitive.fromByteArray(envelope)).getContent());
            KeyTransRecipientInfo ana = KeyTransRecipientInfo.getInstance(
                    RecipientInfo.getInstance(data.getRecipientInfos().getObjectAt(0)).getInfo());
            BigInteger serial = IssuerAndSerialNumber.getInstance(ana.getRecipientIdentifier().getId()).getSerialNumber().getValue();
            EncryptedContentInfo content = data.getEncryptedContentInfo();
            byte[] iv = ASN1OctetString.getInstance(content.getContentEncryptionAlgorithm().getParameters()).getOctets();
            return new Parts(serial, ana.getEncryptedKey().getOctets(), iv, content.getEncryptedContent().getOctets());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Steps two and three: unlock the AES key with the private key, then decrypt and remove the padding. */
    public static Opened open(PrivateKey key, Parts parts) {
        try {
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding", "SunJCE");
            rsa.init(Cipher.DECRYPT_MODE, key, new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] dataKey = rsa.doFinal(parts.lockedKey());

            Cipher aes = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new IvParameterSpec(parts.iv()));
            byte[] padded = aes.doFinal(parts.cipherText());
            int pad = padded[padded.length - 1];
            return new Opened(dataKey, padded, Arrays.copyOf(padded, padded.length - pad));
        } catch (GeneralSecurityException e) {
            throw new CmsEnvelope.NotForThisKeyException("this private key cannot unlock the AES key");
        }
    }
}
