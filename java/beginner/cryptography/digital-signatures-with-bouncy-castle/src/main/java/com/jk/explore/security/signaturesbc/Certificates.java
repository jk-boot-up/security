package com.jk.explore.security.signaturesbc;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
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

/** An ECDSA P-256 key pair and a self-signed certificate for it, with fixed dates so the demo text is the same on every run. */
final class Certificates {

    static final Instant NOT_BEFORE = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant NOT_AFTER = Instant.parse("2027-01-01T00:00:00Z");

    private Certificates() {
    }

    static KeyPair newEcKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static X509Certificate selfSigned(String commonName, KeyPair keys) {
        Providers.ensure();
        try {
            X500Name name = new X500Name("O=Example Bank, CN=" + commonName);
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(NOT_BEFORE),
                    Date.from(NOT_AFTER), name, keys.getPublic());
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(Providers.BC).build(keys.getPrivate());
            return new JcaX509CertificateConverter().setProvider(Providers.BC).getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
