package com.jk.explore.security.certlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CrlsAndVerdictTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void theDatabaseHasOneLinePerRevokedCertificateWithSixFields() {
        String index = Crls.index(5, 2, NOW);
        String[] lines = index.split("\n");
        assertEquals(5, lines.length);
        for (String line : lines) {
            assertEquals(6, line.split("\t").length);
            assertTrue(line.startsWith("R\t"));
        }
    }

    @Test
    void serialsAreDistinctAndTheFirstOnesBelongToExpiredCertificates() {
        String[] lines = Crls.index(4, 2, NOW).split("\n");
        assertTrue(lines[0].split("\t")[3].endsWith("1000"));
        assertTrue(lines[3].split("\t")[3].endsWith("1003"));
        assertTrue(lines[0].split("\t")[1].compareTo(lines[3].split("\t")[1]) < 0, "expired entries carry an earlier expiry date");
        assertEquals(13, lines[0].split("\t")[1].length());
    }

    @Test
    void anEmptyDatabaseIsEmpty() {
        assertEquals("", Crls.index(0, 0, NOW));
    }

    @Test
    void aVerdictSaysWhy() {
        assertEquals("accepted", Verdict.OK.word());
        assertFalse(new Verdict(false, "revoked").accepted());
        assertEquals("refused (revoked)", new Verdict(false, "revoked").word());
    }

    @Test
    void theTwoPoliciesDifferOnlyInSoftFail() {
        assertTrue(Chains.Policy.HARD.options.isEmpty());
        assertEquals(1, Chains.Policy.SOFT.options.size());
    }

    @Test
    void asn1TimesUseTheFormatOpensslAsksFor() {
        assertEquals("20270101000000Z", Pki.asn1(NOW));
    }

    @Test
    void countsOfALoadRun() {
        Load.Tally t = new Load.Tally(10, 4, NOW, "x");
        assertEquals(6, t.accepted());
    }

    @Test
    void opensslVersionLinesAreRecognised() {
        assertTrue(Openssl.isModern("OpenSSL 3.6.0 1 Oct 2026"));
        assertTrue(Openssl.isModern("OpenSSL 4.0.3 29 Sep 2026"));
        assertFalse(Openssl.isModern("LibreSSL 3.3.6"));
    }
}
