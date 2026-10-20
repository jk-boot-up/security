package com.jk.explore.security.ca;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CaTest {

    private static final Instant NOW = CaDemo.NOW;
    private static LocalCa ca;
    private static KeyPair shopKey;
    private static X509Certificate cert;
    private static X509CRL emptyList;

    @BeforeAll
    static void setUp() {
        ca = new LocalCa("Test CA", NOW);
        shopKey = LocalCa.newKey();
        cert = ca.issue("shop.example", CaDemo.NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        emptyList = ca.publishCrl(NOW, Duration.ofDays(1));
    }

    private static String refusal(X509Certificate c, String host, List<X509CRL> crls, Instant at) {
        return assertThrows(Verifier.Untrusted.class, () -> new Verifier(ca.rootCertificate()).check(List.of(c), host, crls, at)).getMessage();
    }

    @Test
    void derEncodesKnownValues() {
        assertArrayEquals(new byte[] {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x0b}, Der.oid("1.2.840.113549.1.1.11"));
        assertArrayEquals(new byte[] {0x02, 0x02, 0x00, (byte) 0x80}, Der.integer(128));
        assertArrayEquals(new byte[] {0x02, 0x01, 0x05}, Der.integer(5));
        byte[] big = Der.tlv(0x04, new byte[200]);
        assertEquals(0x81, big[1] & 0xff);
        assertEquals(200, big[2] & 0xff);
    }

    @Test
    void theJdkReadsWhatTheCaBuilds() {
        assertEquals(3, cert.getVersion());
        assertEquals(BigInteger.valueOf(1000), cert.getSerialNumber());
        assertEquals("CN=Test CA", cert.getIssuerX500Principal().getName());
        assertEquals(-1, cert.getBasicConstraints());
        assertTrue(ca.rootCertificate().getBasicConstraints() >= 0);
        assertEquals(List.of("1.3.6.1.5.5.7.3.1"), certEku(cert));
    }

    private static List<String> certEku(X509Certificate c) {
        try {
            return c.getExtendedKeyUsage();
        } catch (java.security.cert.CertificateParsingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void aCertificateForTheNameIsAccepted() {
        new Verifier(ca.rootCertificate()).check(List.of(cert), "shop.example", List.of(emptyList), NOW);
        new Verifier(ca.rootCertificate()).check(List.of(cert), "WWW.SHOP.EXAMPLE", List.of(emptyList), NOW);
    }

    @Test
    void namesMustMatchTheSubjectAlternativeNameExactlyOrByOneWildcardLabel() {
        assertTrue(refusal(cert, "a.b.shop.example", List.of(emptyList), NOW).contains("not in the certificate"));
        assertTrue(refusal(cert, "shop.example.evil.example", List.of(emptyList), NOW).contains("not in the certificate"));
        assertTrue(refusal(cert, "evilshop.example", List.of(emptyList), NOW).contains("not in the certificate"));
        assertFalse(Verifier.nameMatches(cert, ".shop.example"));
    }

    @Test
    void theCommonNameAloneIsNotEnough() {
        X509Certificate noSan = ca.issue("shop.example", List.of(), shopKey.getPublic(), NOW, Duration.ofDays(1));
        assertEquals("certificate is not for a TLS server", refusal(noSan, "shop.example", List.of(emptyList), NOW));
    }

    @Test
    void datesAreCheckedAtTheGivenMoment() {
        Instant late = NOW.plus(Duration.ofDays(100));
        assertEquals("certificate expired", refusal(cert, "shop.example", List.of(ca.publishCrl(late, Duration.ofDays(1))), late));
        Instant early = NOW.minus(Duration.ofDays(1));
        assertEquals("certificate not yet valid", refusal(cert, "shop.example", List.of(ca.publishCrl(early, Duration.ofDays(1))), early));
    }

    @Test
    void anotherAuthorityOrAForgedSignatureOrASelfSignedCertificateIsRefused() {
        LocalCa other = new LocalCa("Other CA", NOW);
        assertEquals("not issued by a trusted authority", refusal(other.issue("shop.example", CaDemo.NAMES, shopKey.getPublic(), NOW, Duration.ofDays(1)), "shop.example", List.of(emptyList), NOW));
        assertEquals("signature does not match the issuer", refusal(ca.forge("shop.example", CaDemo.NAMES, shopKey.getPublic(), LocalCa.newKey(), NOW), "shop.example", List.of(emptyList), NOW));
    }

    @Test
    void aLeafCannotSignOtherCertificates() {
        KeyPair victim = LocalCa.newKey();
        X509Certificate under = Certificates.build(new Certificates.Spec("bank.example", "shop.example", BigInteger.valueOf(7), NOW.minusSeconds(60), NOW.plus(Duration.ofDays(1)), false,
                List.of("bank.example")), victim.getPublic(), shopKey.getPrivate());
        var e = assertThrows(Verifier.Untrusted.class, () -> new Verifier(ca.rootCertificate()).check(List.of(under, cert), "bank.example", List.of(emptyList), NOW));
        assertEquals("a certificate in the chain may not sign certificates", e.getMessage());
    }

    @Test
    void revocationNeedsACurrentListAndIsHardFail() {
        LocalCa own = new LocalCa("Revoker", NOW);
        X509Certificate c = own.issue("shop.example", CaDemo.NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        Instant t = NOW.plus(Duration.ofDays(10));
        own.revoke(c, t);
        X509CRL list = own.publishCrl(t, Duration.ofDays(1));
        Verifier v = new Verifier(own.rootCertificate());
        var revoked = assertThrows(Verifier.Untrusted.class, () -> v.check(List.of(c), "shop.example", List.of(list), t.plusSeconds(1)));
        assertEquals("certificate revoked", revoked.getMessage());
        assertTrue(list.isRevoked(c));
        var none = assertThrows(Verifier.Untrusted.class, () -> v.check(List.of(c), "shop.example", List.of(), t.plusSeconds(1)));
        assertTrue(none.getMessage().startsWith("revocation status unknown"));
        var stale = assertThrows(Verifier.Untrusted.class, () -> v.check(List.of(c), "shop.example", List.of(list), t.plus(Duration.ofDays(2))));
        assertTrue(stale.getMessage().startsWith("revocation status unknown"));
    }

    @Test
    void aListFromAnotherAuthorityDoesNotCount() {
        LocalCa other = new LocalCa("Test CA", NOW);
        assertTrue(refusal(cert, "shop.example", List.of(other.publishCrl(NOW, Duration.ofDays(1))), NOW).startsWith("revocation status unknown")
                || refusal(cert, "shop.example", List.of(other.publishCrl(NOW, Duration.ofDays(1))), NOW).contains("signature"));
    }

    @Test
    void renewalKeepsTheKeyAndChangesSerialAndDates() {
        LocalCa own = new LocalCa("Renewer", NOW);
        X509Certificate first = own.issue("shop.example", CaDemo.NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        X509Certificate renewed = own.renew(first, CaDemo.NAMES, NOW.plus(Duration.ofDays(80)), Duration.ofDays(90));
        assertEquals(first.getPublicKey(), renewed.getPublicKey());
        assertFalse(first.getSerialNumber().equals(renewed.getSerialNumber()));
        assertTrue(renewed.getNotAfter().after(first.getNotAfter()));
        assertEquals("CN=shop.example", renewed.getSubjectX500Principal().getName());
    }
}
