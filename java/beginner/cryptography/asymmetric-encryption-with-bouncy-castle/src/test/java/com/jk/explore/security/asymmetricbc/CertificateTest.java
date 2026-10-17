package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CertificateTest {

    private static KeyPair keys;
    private static X509Certificate cert;

    @BeforeAll
    static void make() {
        keys = BouncyCastleDemo.newKeyPair();
        cert = Certificates.selfSigned("Ana", keys);
    }

    @Test
    void theCertificateNamesTheSubjectAndCarriesThePublicKey() {
        assertEquals("CN=Ana,O=Example Bank customer", cert.getSubjectX500Principal().getName());
        assertEquals(keys.getPublic(), cert.getPublicKey());
    }

    @Test
    void itIsSelfSigned() throws Exception {
        assertEquals(cert.getIssuerX500Principal(), cert.getSubjectX500Principal());
        cert.verify(keys.getPublic());
    }

    @Test
    void itIsValidForOneYearFromAFixedDate() {
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), cert.getNotBefore().toInstant());
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), cert.getNotAfter().toInstant());
    }

    @Test
    void itAllowsKeyEnciphermentAndSignatures() {
        boolean[] usage = cert.getKeyUsage();
        assertTrue(usage[0], "digitalSignature");
        assertTrue(usage[2], "keyEncipherment");
    }

    @Test
    void itRoundTripsThroughPem() {
        assertEquals(cert, Pem.readCertificate(Pem.certificate(cert)));
    }
}
