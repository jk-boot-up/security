package com.jk.explore.security.certsbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ListAndRequestTest {

    private static Pki pki;
    private static ChainValidator browser;
    private static final Instant YESTERDAY = Pki.TODAY.minusSeconds(86400);

    @BeforeAll
    static void make() {
        pki = new Pki();
        browser = new ChainValidator(Set.of(pki.root.certificate()));
    }

    private List<X509Certificate> chain() {
        return List.of(pki.server.certificate(), pki.intermediate.certificate());
    }

    @Test
    void emptyListsAcceptTheChain() {
        List<X509CRL> lists = List.of(Crls.empty(pki.root, YESTERDAY), Crls.empty(pki.intermediate, YESTERDAY));
        assertTrue(browser.validateWithLists(chain(), Pki.TODAY, lists).accepted());
    }

    @Test
    void listedSerialIsRevoked() {
        X509CRL list = Crls.issue(pki.intermediate, YESTERDAY, Map.of(pki.server.certificate().getSerialNumber(), Crls.KEY_COMPROMISE));
        assertEquals(1, Crls.entries(list));
        assertTrue(Crls.lists(list, pki.server.certificate().getSerialNumber()));
        assertFalse(Crls.lists(list, BigInteger.TEN));
        ChainValidator.Result result = browser.validateWithLists(chain(), Pki.TODAY, List.of(Crls.empty(pki.root, YESTERDAY), list));
        assertFalse(result.accepted());
        assertEquals("REVOKED", result.reason());
    }

    @Test
    void revokingTheIntermediateRevokesWhatItIssued() {
        X509CRL rootList = Crls.issue(pki.root, YESTERDAY, Map.of(pki.intermediate.certificate().getSerialNumber(), Crls.CA_COMPROMISE));
        ChainValidator.Result result = browser.validateWithLists(chain(), Pki.TODAY, List.of(rootList, Crls.empty(pki.intermediate, YESTERDAY)));
        assertEquals("REVOKED", result.reason());
    }

    @Test
    void listsAreSignedByTheirIssuerOnly() {
        X509CRL list = Crls.empty(pki.intermediate, YESTERDAY);
        assertTrue(Crls.signedBy(list, pki.intermediate));
        assertFalse(Crls.signedBy(list, pki.root));
    }

    @Test
    void missingOrForeignListsLeaveTheStatusUndetermined() {
        assertEquals("UNDETERMINED_REVOCATION_STATUS", browser.validateWithLists(chain(), Pki.TODAY, List.of()).reason());
        X509CRL foreign = Crls.empty(Pki.rogue()[0], YESTERDAY);
        assertFalse(browser.validateWithLists(chain(), Pki.TODAY, List.of(Crls.empty(pki.root, YESTERDAY), foreign)).accepted());
    }

    @Test
    void anOutOfDateListIsNotEnough() {
        Instant old = Pki.TODAY.minusSeconds(30L * 86400);
        List<X509CRL> lists = List.of(Crls.empty(pki.root, old), Crls.empty(pki.intermediate, old));
        assertFalse(browser.validateWithLists(chain(), Pki.TODAY, lists).accepted());
    }

    @Test
    void requestProvesPossessionAndNamesTheHost() {
        PKCS10CertificationRequest request = SigningRequest.create(CertFactory.newKeys(), "shop.example");
        assertTrue(SigningRequest.provesPossession(request));
        assertEquals(List.of("shop.example"), SigningRequest.requestedNames(request));
    }

    @Test
    void aChangedRequestFailsItsSignature() throws Exception {
        PKCS10CertificationRequest request = SigningRequest.create(CertFactory.newKeys(), "shop.example");
        assertFalse(SigningRequest.provesPossession(SigningRequest.parse(SigningRequest.changeName(request, "shop.example"))));
    }

    @Test
    void authorityCertifiesTheKeyFromTheRequest() throws Exception {
        KeyPair keys = CertFactory.newKeys();
        X509Certificate certificate = SigningRequest.certify(SigningRequest.create(keys, "api.shop.example"), pki.intermediate, "shop.example", 30, LocalDate.of(2025, 6, 1));
        assertEquals(keys.getPublic(), certificate.getPublicKey());
        assertTrue(browser.validate(List.of(certificate, pki.intermediate.certificate()), Pki.TODAY).accepted());
    }

    @Test
    void authorityRefusesOtherDomainsAndLookAlikes() {
        PKCS10CertificationRequest bank = SigningRequest.create(CertFactory.newKeys(), "bank.example");
        assertThrows(SigningRequest.RefusedException.class, () -> SigningRequest.certify(bank, pki.intermediate, "shop.example", 31, LocalDate.of(2025, 6, 1)));
        PKCS10CertificationRequest lookAlike = SigningRequest.create(CertFactory.newKeys(), "evilshop.example");
        assertThrows(SigningRequest.RefusedException.class, () -> SigningRequest.certify(lookAlike, pki.intermediate, "shop.example", 32, LocalDate.of(2025, 6, 1)));
    }

    @Test
    void pemRoundTripsACertificate() {
        String pem = Pem.of(pki.server.certificate());
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----"));
        assertEquals(pki.server.certificate(), Pem.certificate(pem));
        assertTrue(Pem.of(Crls.empty(pki.root, YESTERDAY)).startsWith("-----BEGIN X509 CRL-----"));
        assertTrue(Pem.of(SigningRequest.create(CertFactory.newKeys(), "shop.example")).startsWith("-----BEGIN CERTIFICATE REQUEST-----"));
        assertThrows(IllegalArgumentException.class, () -> Pem.certificate("not pem"));
    }
}
