package com.jk.explore.security.certsbc;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Builds X.509 version 3 certificates with Bouncy Castle's builders. The JDK reads, checks and validates certificates but has no public
 * API to make them; the builders here do what a certificate authority does: name the subject, set the validity, attach the extensions
 * that say what the certificate may be used for, and sign. Keys are EC P-256 and signatures are ECDSA with SHA-256.
 */
final class CertFactory {

    private CertFactory() {
    }

    static KeyPair newKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * What is being asked for. A pathLength of -1 means the holder is not an authority; zero or more is the number of further authorities
     * it may certify below itself. A dnsName becomes a subject alternative name; permittedDns limits the names an authority may certify.
     */
    record Spec(String commonName, LocalDate from, LocalDate to, long serial, int pathLength, String dnsName, String permittedDns) {

        static Spec authority(String commonName, LocalDate from, LocalDate to, long serial, int pathLength) {
            return new Spec(commonName, from, to, serial, pathLength, null, null);
        }

        Spec limitedTo(String permittedDns) {
            return new Spec(commonName, from, to, serial, pathLength, dnsName, permittedDns);
        }

        static Spec server(String dnsName, LocalDate from, LocalDate to, long serial) {
            return new Spec(dnsName, from, to, serial, -1, dnsName, null);
        }

        boolean authority() {
            return pathLength >= 0;
        }
    }

    static Issued selfSigned(Spec spec, KeyPair keys) {
        return new Issued(keys, build(spec, keys.getPublic(), keys.getPrivate(), null));
    }

    /** Issues a certificate for the subject keys, signed by the issuer. */
    static Issued issue(Spec spec, KeyPair subjectKeys, Issued issuer) {
        return new Issued(subjectKeys, issueFor(spec, subjectKeys.getPublic(), issuer));
    }

    /** Issues a certificate for a public key someone else holds, as a certificate authority does after receiving a signing request. */
    static X509Certificate issueFor(Spec spec, PublicKey subjectKey, Issued issuer) {
        return build(spec, subjectKey, issuer.keys().getPrivate(), issuer.certificate());
    }

    private static Date date(LocalDate day) {
        return Date.from(day.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private static X509Certificate build(Spec spec, PublicKey subjectKey, PrivateKey signingKey, X509Certificate issuerCertificate) {
        try {
            X500Name subject = new X500Name("CN=" + spec.commonName());
            X500Name issuerName = issuerCertificate == null ? subject : new JcaX509CertificateHolder(issuerCertificate).getSubject();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, BigInteger.valueOf(spec.serial()),
                    date(spec.from()), date(spec.to()), subject, subjectKey);
            JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(subjectKey));
            builder.addExtension(Extension.authorityKeyIdentifier, false,
                    issuerCertificate == null ? utils.createAuthorityKeyIdentifier(subjectKey) : utils.createAuthorityKeyIdentifier(issuerCertificate));
            if (spec.authority()) {
                builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(spec.pathLength()));
                builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            } else {
                builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
                builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
                builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            }
            if (spec.dnsName() != null) {
                builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, spec.dnsName())));
            }
            if (spec.permittedDns() != null) {
                GeneralSubtree permitted = new GeneralSubtree(new GeneralName(GeneralName.dNSName, spec.permittedDns()));
                builder.addExtension(Extension.nameConstraints, true, new NameConstraints(new GeneralSubtree[] {permitted}, null));
            }
            return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signingKey)));
        } catch (Exception e) {
            throw new IllegalStateException("could not build the certificate for " + spec.commonName(), e);
        }
    }
}
