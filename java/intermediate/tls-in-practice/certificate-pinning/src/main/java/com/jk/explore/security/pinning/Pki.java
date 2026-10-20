package com.jk.explore.security.pinning;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Just enough certificate making to build chains for the demo: a root, an intermediate and a leaf, as real X.509 DER read back by the JDK. */
public final class Pki {

    /** A certificate with the key pair that belongs to it. */
    public record Identity(String name, KeyPair keys, X509Certificate certificate) {
    }

    private static long serial = 1;

    private Pki() {
    }

    public static KeyPair newKey() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Identity root(String name, Instant now) {
        KeyPair k = newKey();
        return new Identity(name, k, build(name, name, k.getPublic(), k, now, Duration.ofDays(3650), true, List.of()));
    }

    public static Identity intermediate(String name, Identity issuer, Instant now) {
        KeyPair k = newKey();
        return new Identity(name, k, build(name, issuer.name(), k.getPublic(), issuer.keys(), now, Duration.ofDays(1800), true, List.of()));
    }

    /** A server certificate for {@code dnsNames}, for the given key (so a renewal can keep or change the key). */
    public static X509Certificate leaf(String name, Identity issuer, KeyPair serverKey, Instant now, Duration life, List<String> dnsNames) {
        return build(name, issuer.name(), serverKey.getPublic(), issuer.keys(), now, life, false, dnsNames);
    }

    private static X509Certificate build(String subject, String issuer, PublicKey subjectKey, KeyPair signer, Instant now, Duration life, boolean ca, List<String> dns) {
        try {
            List<byte[]> ext = new ArrayList<>();
            ext.add(Der.sequence(Der.oid("2.5.29.19"), Der.bool(true), Der.octets(ca ? Der.sequence(Der.bool(true)) : Der.sequence())));
            ext.add(Der.sequence(Der.oid("2.5.29.15"), Der.bool(true), Der.octets(ca ? Der.bits(1, (byte) 0x06) : Der.bits(7, (byte) 0x80))));
            if (!dns.isEmpty()) {
                ext.add(Der.sequence(Der.oid("2.5.29.17"), Der.octets(Der.sequence(dns.stream().map(n -> Der.contextString(2, n)).toArray(byte[][]::new)))));
            }
            byte[] sigAlg = Der.sequence(Der.oid("1.2.840.10045.4.3.2"));
            byte[] tbs = Der.sequence(Der.explicit(0, Der.integer(2)), Der.integer(BigInteger.valueOf(serial++)), sigAlg, Der.commonName(issuer),
                    Der.sequence(Der.time(now.minus(Duration.ofMinutes(1))), Der.time(now.plus(life))), Der.commonName(subject), subjectKey.getEncoded(),
                    Der.explicit(3, Der.sequence(ext.toArray(byte[][]::new))));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(signer.getPrivate());
            s.update(tbs);
            byte[] cert = Der.sequence(tbs, sigAlg, Der.bits(0, s.sign()));
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
