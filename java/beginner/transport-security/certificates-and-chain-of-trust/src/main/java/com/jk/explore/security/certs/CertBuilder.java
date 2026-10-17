package com.jk.explore.security.certs;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;

/**
 * Builds X.509 version 3 certificates by writing their DER encoding directly and signing it with the JDK. The JDK reads, checks and
 * validates certificates but has no public API to make them, so this small builder fills the gap, which is the part a certificate
 * authority performs. Keys are EC P-256 and signatures are ECDSA with SHA-256.
 */
final class CertBuilder {

    private static final String ECDSA_SHA256 = "1.2.840.10045.4.3.2";

    private CertBuilder() {
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

    /** What is being asked for: a name, a validity period and whether the holder may itself issue certificates. */
    record Request(String commonName, LocalDate from, LocalDate to, long serial, boolean authority, Integer pathLength, String dnsName) {

        static Request authority(String commonName, LocalDate from, LocalDate to, long serial, int pathLength) {
            return new Request(commonName, from, to, serial, true, pathLength, null);
        }

        static Request server(String dnsName, LocalDate from, LocalDate to, long serial) {
            return new Request(dnsName, from, to, serial, false, null, dnsName);
        }
    }

    /** Issues a certificate for the subject keys, signed by the issuer. An issuer of null makes a self-signed certificate. */
    static Issued issue(Request request, KeyPair subjectKeys, Issued issuer) {
        byte[] subjectName = name(request.commonName());
        byte[] issuerName = issuer == null ? subjectName : issuer.certificate().getSubjectX500Principal().getEncoded();
        KeyPair signer = issuer == null ? subjectKeys : issuer.keys();
        byte[] algorithm = Der.sequence(Der.oid(ECDSA_SHA256));
        byte[] tbs = Der.sequence(
                Der.explicit(0, Der.integer(2)),
                Der.integer(request.serial()),
                algorithm,
                issuerName,
                Der.sequence(Der.utcTime(request.from().atStartOfDay()), Der.utcTime(request.to().atStartOfDay())),
                subjectName,
                subjectKeys.getPublic().getEncoded(),
                Der.explicit(3, Der.sequence(extensions(request))));
        byte[] certificate = Der.sequence(tbs, algorithm, Der.bitString(0, sign(signer, tbs)));
        return new Issued(subjectKeys, parse(certificate));
    }

    private static byte[] name(String commonName) {
        return Der.sequence(Der.set(Der.sequence(Der.oid("2.5.4.3"), Der.utf8(commonName))));
    }

    private static byte[][] extensions(Request request) {
        byte[] basic = request.authority()
                ? Der.sequence(Der.bool(true), Der.integer(request.pathLength()))
                : Der.sequence();
        byte[] usage = request.authority() ? Der.bitString(1, (byte) 0x06) : Der.bitString(7, (byte) 0x80);
        byte[] basicExtension = extension("2.5.29.19", true, basic);
        byte[] usageExtension = extension("2.5.29.15", true, usage);
        if (request.dnsName() == null) {
            return new byte[][] {basicExtension, usageExtension};
        }
        byte[] alternative = extension("2.5.29.17", false, Der.sequence(Der.ia5(0x82, request.dnsName())));
        return new byte[][] {basicExtension, usageExtension, alternative};
    }

    private static byte[] extension(String oid, boolean critical, byte[] value) {
        return critical
                ? Der.sequence(Der.oid(oid), Der.bool(true), Der.octets(value))
                : Der.sequence(Der.oid(oid), Der.octets(value));
    }

    private static byte[] sign(KeyPair signer, byte[] data) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(signer.getPrivate());
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static X509Certificate parse(byte[] der) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("not a certificate", e);
        }
    }
}
