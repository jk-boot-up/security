package com.jk.explore.security.ca;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds X.509 version 3 certificates and version 2 revocation lists as real DER, signed with ECDSA and SHA-256, and reads them back with the JDK. */
final class Certificates {

    private static final String ECDSA_SHA256 = "1.2.840.10045.4.3.2";

    private Certificates() {
    }

    /** What goes into a certificate besides the key. */
    record Spec(String subject, String issuer, BigInteger serial, Instant notBefore, Instant notAfter, boolean ca, List<String> dnsNames) {
    }

    static X509Certificate build(Spec spec, PublicKey subjectKey, PrivateKey signer) {
        try {
            List<byte[]> extensions = new ArrayList<>();
            extensions.add(extension("2.5.29.19", true, spec.ca() ? Der.sequence(Der.bool(true)) : Der.sequence())); // basicConstraints: may this key sign certificates?
            extensions.add(extension("2.5.29.15", true, spec.ca() ? Der.bits(1, (byte) 0x06) : Der.bits(7, (byte) 0x80))); // keyCertSign+cRLSign, or digitalSignature
            if (!spec.dnsNames().isEmpty()) {
                byte[][] names = spec.dnsNames().stream().map(n -> Der.contextString(2, n)).toArray(byte[][]::new);
                extensions.add(extension("2.5.29.17", false, Der.sequence(names))); // subjectAltName: the names this certificate is good for
                extensions.add(extension("2.5.29.37", false, Der.sequence(Der.oid("1.3.6.1.5.5.7.3.1")))); // extendedKeyUsage: TLS server
            }
            byte[] tbs = Der.sequence(
                    Der.explicit(0, Der.integer(2)),
                    Der.integer(spec.serial()),
                    Der.sequence(Der.oid(ECDSA_SHA256)),
                    Der.commonName(spec.issuer()),
                    Der.sequence(Der.time(spec.notBefore()), Der.time(spec.notAfter())),
                    Der.commonName(spec.subject()),
                    subjectKey.getEncoded(),
                    Der.explicit(3, Der.sequence(extensions.toArray(byte[][]::new))));
            byte[] cert = Der.sequence(tbs, Der.sequence(Der.oid(ECDSA_SHA256)), Der.bits(0, sign(tbs, signer)));
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A revocation list: the serial numbers this issuer no longer stands behind, valid from {@code thisUpdate} until {@code nextUpdate}. */
    static X509CRL crl(String issuer, PrivateKey signer, Instant thisUpdate, Instant nextUpdate, long number, Map<BigInteger, Instant> revoked) {
        try {
            List<byte[]> parts = new ArrayList<>();
            parts.add(Der.integer(1));
            parts.add(Der.sequence(Der.oid(ECDSA_SHA256)));
            parts.add(Der.commonName(issuer));
            parts.add(Der.time(thisUpdate));
            parts.add(Der.time(nextUpdate));
            if (!revoked.isEmpty()) {
                byte[][] entries = revoked.entrySet().stream().map(e -> Der.sequence(Der.integer(e.getKey()), Der.time(e.getValue()))).toArray(byte[][]::new);
                parts.add(Der.sequence(entries));
            }
            parts.add(Der.explicit(0, Der.sequence(extension("2.5.29.20", false, Der.integer(number))))); // cRLNumber
            byte[] tbs = Der.sequence(parts.toArray(byte[][]::new));
            byte[] crl = Der.sequence(tbs, Der.sequence(Der.oid(ECDSA_SHA256)), Der.bits(0, sign(tbs, signer)));
            return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(crl));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] extension(String oid, boolean critical, byte[] value) {
        return critical ? Der.sequence(Der.oid(oid), Der.bool(true), Der.octets(value)) : Der.sequence(Der.oid(oid), Der.octets(value));
    }

    private static byte[] sign(byte[] data, PrivateKey key) throws GeneralSecurityException {
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(key);
        s.update(data);
        return s.sign();
    }
}
