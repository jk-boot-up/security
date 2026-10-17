package com.jk.explore.security.asymmetricbc;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Makes a self-signed X.509 certificate: a public key, a name, a validity period, all signed with the matching private key.
 * Anyone can make one, so by itself it proves nothing about who the owner is: that needs a signature from someone else who
 * is trusted, which is the subject of the digital-signatures and certificates projects.
 */
public final class Certificates {

    /** A fixed start date and a one-year life, so that the demo's text is the same on every run. */
    static final Instant NOT_BEFORE = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant NOT_AFTER = Instant.parse("2027-01-01T00:00:00Z");

    private Certificates() {
    }

    public static X509Certificate selfSigned(String commonName, KeyPair keys) {
        Providers.ensure();
        try {
            X500Name name = new X500Name("O=Example Bank customer, CN=" + commonName);
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(NOT_BEFORE),
                    Date.from(NOT_AFTER), name, keys.getPublic());
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.digitalSignature));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(Providers.BC).build(keys.getPrivate());
            return new JcaX509CertificateConverter().setProvider(Providers.BC).getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
