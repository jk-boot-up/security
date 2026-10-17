package com.jk.explore.security.certsbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.x509.Extension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChainTest {

    private static Pki pki;
    private static ChainValidator browser;

    @BeforeAll
    static void make() {
        pki = new Pki();
        browser = new ChainValidator(Set.of(pki.root.certificate()));
    }

    private List<X509Certificate> chain(Issued server) {
        return List.of(server.certificate(), pki.intermediate.certificate());
    }

    @Test
    void goodChainIsAccepted() {
        assertTrue(browser.validate(chain(pki.server), Pki.TODAY).accepted());
    }

    @Test
    void extensionsDescribeEachRole() {
        assertEquals(1, pki.root.certificate().getBasicConstraints());
        assertEquals(0, pki.intermediate.certificate().getBasicConstraints());
        assertEquals(-1, pki.server.certificate().getBasicConstraints());
        assertTrue(pki.intermediate.certificate().getKeyUsage()[5]);
        assertFalse(pki.server.certificate().getKeyUsage()[5]);
        assertTrue(pki.server.certificate().getKeyUsage()[0]);
        assertTrue(pki.server.certificate().getCriticalExtensionOIDs().contains(Extension.basicConstraints.getId()));
        assertNotNull(pki.server.certificate().getExtensionValue(Extension.subjectAlternativeName.getId()));
    }

    @Test
    void keyIdentifiersLinkTheChain() {
        assertTrue(CertsBcDemo.linkedTo(pki.server.certificate(), pki.intermediate.certificate()));
        assertTrue(CertsBcDemo.linkedTo(pki.intermediate.certificate(), pki.root.certificate()));
        assertFalse(CertsBcDemo.linkedTo(pki.server.certificate(), pki.root.certificate()));
    }

    @Test
    void expiredCertificateIsRefused() {
        ChainValidator.Result result = browser.validate(chain(pki.expiredServer()), Pki.TODAY);
        assertFalse(result.accepted());
        assertEquals("EXPIRED", result.reason());
    }

    @Test
    void chainToAnUntrustedRootIsRefused() {
        Issued[] rogue = Pki.rogue();
        ChainValidator.Result result = browser.validate(List.of(rogue[1].certificate()), Pki.TODAY);
        assertFalse(result.accepted());
    }

    @Test
    void nameConstraintAllowsTheStoreAndItsSubdomains() {
        assertTrue(browser.validate(chain(pki.serverFor("api.shop.example", 5)), Pki.TODAY).accepted());
    }

    @Test
    void nameConstraintRefusesOtherNames() {
        ChainValidator.Result result = browser.validate(chain(pki.serverFor("other.example", 6)), Pki.TODAY);
        assertFalse(result.accepted());
        assertEquals("INVALID_NAME", result.reason());
        assertFalse(browser.validate(chain(pki.serverFor("shop.example.evil.test", 7)), Pki.TODAY).accepted());
    }

    @Test
    void theIntermediateCannotCertifyAnotherAuthority() {
        Issued sub = CertFactory.issue(CertFactory.Spec.authority("Sub CA", Pki.START, java.time.LocalDate.of(2030, 1, 1), 20, 0).limitedTo("shop.example"),
                CertFactory.newKeys(), pki.intermediate);
        Issued leaf = CertFactory.issue(CertFactory.Spec.server("shop.example", Pki.START, java.time.LocalDate.of(2026, 6, 1), 21), CertFactory.newKeys(), sub);
        ChainValidator.Result result = browser.validate(List.of(leaf.certificate(), sub.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        assertFalse(result.accepted(), "the intermediate's path length of zero forbids a further authority");
    }

    @Test
    void aChangedCertificateFailsItsSignature() throws Exception {
        byte[] bytes = pki.server.certificate().getEncoded();
        byte[] needle = "shop.example".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int at = 0;
        outer:
        for (; at + needle.length <= bytes.length; at++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[at + j] != needle[j]) {
                    continue outer;
                }
            }
            break;
        }
        bytes[at + needle.length - 1] -= 32;
        X509Certificate changed = (X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(bytes));
        assertFalse(browser.validate(List.of(changed, pki.intermediate.certificate()), Pki.TODAY).accepted());
    }
}
