package com.jk.explore.security.certs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChainTest {

    private final Pki pki = new Pki();
    private final ChainValidator browser = new ChainValidator(Set.of(pki.root.certificate()), Set.of());

    private List<X509Certificate> good() {
        return List.of(pki.server.certificate(), pki.intermediate.certificate());
    }

    @Test
    void certificatesParseWithTheExpectedFields() throws Exception {
        X509Certificate leaf = pki.server.certificate();
        assertEquals(3, leaf.getVersion());
        assertEquals(BigInteger.valueOf(3), leaf.getSerialNumber());
        assertEquals("CN=shop.example", leaf.getSubjectX500Principal().getName());
        assertEquals("CN=Example Issuing CA", leaf.getIssuerX500Principal().getName());
        assertEquals(-1, leaf.getBasicConstraints());
        assertEquals(0, pki.intermediate.certificate().getBasicConstraints());
        assertEquals(1, pki.root.certificate().getBasicConstraints());
        assertEquals("SHA256withECDSA", leaf.getSigAlgName());
        assertTrue(leaf.getKeyUsage()[0]);
        assertTrue(pki.root.certificate().getKeyUsage()[5]);
    }

    @Test
    void theRootIsSelfSignedAndTheJdkConfirmsIt() throws Exception {
        pki.root.certificate().verify(pki.root.certificate().getPublicKey());
        pki.server.certificate().verify(pki.intermediate.certificate().getPublicKey());
    }

    @Test
    void goodChainIsAccepted() {
        assertTrue(browser.validate(good(), Pki.TODAY).accepted());
    }

    @Test
    void expiredAndNotYetValidAreRefused() {
        Issued expired = pki.expiredServer();
        assertEquals("EXPIRED", browser.validate(List.of(expired.certificate(), pki.intermediate.certificate()), Pki.TODAY).reason());
        assertEquals("NOT_YET_VALID", browser.validate(good(), Instant.parse("2025-03-01T00:00:00Z")).reason());
        assertEquals("EXPIRED", browser.validate(good(), Instant.parse("2026-07-01T00:00:00Z")).reason());
    }

    @Test
    void chainToAnotherRootIsRefusedEvenWithTheSameName() {
        Issued[] rogue = Pki.rogue();
        assertEquals(pki.root.certificate().getSubjectX500Principal(), rogue[0].certificate().getSubjectX500Principal());
        assertFalse(browser.validate(List.of(rogue[1].certificate()), Pki.TODAY).accepted());
        assertTrue(new ChainValidator(Set.of(rogue[0].certificate()), Set.of()).validate(List.of(rogue[1].certificate()), Pki.TODAY).accepted());
    }

    @Test
    void missingIntermediateIsRefused() {
        assertFalse(browser.validate(List.of(pki.server.certificate()), Pki.TODAY).accepted());
    }

    @Test
    void changedCertificateIsRefused() {
        X509Certificate changed = Tamper.changeName(pki.server.certificate());
        assertNotEquals(pki.server.certificate(), changed);
        assertEquals("INVALID_SIGNATURE", browser.validate(List.of(changed, pki.intermediate.certificate()), Pki.TODAY).reason());
    }

    @Test
    void aServerCertificateCannotIssueCertificates() {
        Issued forged = CertBuilder.issue(CertBuilder.Request.server("other.example", Pki.START, LocalDate.of(2026, 6, 1), 50), CertBuilder.newKeys(), pki.server);
        ChainValidator.Result result = browser.validate(List.of(forged.certificate(), pki.server.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        assertFalse(result.accepted());
    }

    @Test
    void pathLengthLimitsHowManyAuthoritiesMayFollow() {
        Issued second = CertBuilder.issue(CertBuilder.Request.authority("Too Deep CA", Pki.START, LocalDate.of(2030, 1, 1), 60, 0), CertBuilder.newKeys(), pki.intermediate);
        Issued leaf = CertBuilder.issue(CertBuilder.Request.server("deep.example", Pki.START, LocalDate.of(2026, 6, 1), 61), CertBuilder.newKeys(), second);
        assertFalse(browser.validate(List.of(leaf.certificate(), second.certificate(), pki.intermediate.certificate()), Pki.TODAY).accepted());
    }

    @Test
    void revokedSerialsAreRefused() {
        ChainValidator revoking = new ChainValidator(Set.of(pki.root.certificate()), Set.of(BigInteger.valueOf(3)));
        ChainValidator.Result result = revoking.validate(good(), Pki.TODAY);
        assertFalse(result.accepted());
        assertEquals("REVOKED", result.reason());
        ChainValidator other = new ChainValidator(Set.of(pki.root.certificate()), Set.of(BigInteger.valueOf(99)));
        assertTrue(other.validate(good(), Pki.TODAY).accepted());
    }

    @Test
    void aStolenIssuerKeyForgesAcceptedCertificatesUntilRevoked() {
        Issued stolen = CertBuilder.issue(CertBuilder.Request.server(Pki.HOST, Pki.START, LocalDate.of(2026, 6, 1), 77), CertBuilder.newKeys(), pki.intermediate);
        List<X509Certificate> forged = List.of(stolen.certificate(), pki.intermediate.certificate());
        assertTrue(browser.validate(forged, Pki.TODAY).accepted());
        ChainValidator revoking = new ChainValidator(Set.of(pki.root.certificate()), Set.of(pki.intermediate.certificate().getSerialNumber()));
        assertFalse(revoking.validate(forged, Pki.TODAY).accepted());
    }
}
