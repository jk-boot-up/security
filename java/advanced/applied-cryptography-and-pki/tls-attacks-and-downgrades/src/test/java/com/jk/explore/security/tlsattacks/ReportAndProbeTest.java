package com.jk.explore.security.tlsattacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReportAndProbeTest {

    static final String WEAK_REPORT = """
        [ {"id":"TLS1","ip":"x","port":"1","severity":"LOW","finding":"offered (deprecated)"},
          {"id":"TLS1_1","ip":"x","port":"1","severity":"LOW","finding":"offered (deprecated)"},
          {"id":"TLS1_2","ip":"x","port":"1","severity":"OK","finding":"offered"},
          {"id":"LUCKY13","ip":"x","port":"1","severity":"LOW","finding":"potentially vulnerable, uses obsolete TLS CBC ciphers"},
          {"id":"cipher-tls1_2_x9c","ip":"x","port":"1","severity":"OK","finding":"TLSv1.2   x9c     AES128-GCM-SHA256   RSA   AESGCM  128  TLS_RSA_WITH_AES_128_GCM_SHA256"},
          {"id":"cipher-tls1_2_x33","ip":"x","port":"1","severity":"LOW","finding":"TLSv1.2   x33     DHE-RSA-AES128-SHA   DH 1024   AES  128  TLS_DHE_RSA_WITH_AES_128_CBC_SHA"},
          {"id":"cipher-tls1_2_x02","ip":"x","port":"1","severity":"CRITICAL","finding":"TLSv1.2   x02     NULL-SHA   RSA   None  None  TLS_RSA_WITH_NULL_SHA"} ]
        """;

    static final String STRONG_REPORT = """
        [ {"id":"TLS1","ip":"x","port":"1","severity":"OK","finding":"not offered"},
          {"id":"TLS1_2","ip":"x","port":"1","severity":"OK","finding":"offered"},
          {"id":"cipher-tls1_2_xc02f","ip":"x","port":"1","severity":"OK","finding":"TLSv1.2   xc02f   ECDHE-RSA-AES128-GCM-SHA256   ECDH 253   AESGCM  128  TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"} ]
        """;

    @Test
    void theJsonReportIsReadAndTheWeakServerShowsAllSixPlantedWeaknesses() {
        List<TestSsl.Finding> findings = TestSsl.parse(WEAK_REPORT);
        assertEquals(7, findings.size());
        assertEquals(EnumSet.allOf(Weakness.class), TestSsl.detect(findings));
        assertEquals(List.of("AES128-GCM-SHA256", "NULL-SHA"), TestSsl.kx(findings, "RSA"));
    }

    @Test
    void theReportSaysWhereEachWeaknessShows() {
        var where = TestSsl.evidence(TestSsl.parse(WEAK_REPORT));
        assertEquals("finding TLS1, LOW", where.get(Weakness.TLS_1_0));
        assertEquals("finding LUCKY13, LOW", where.get(Weakness.CBC_SHA1));
        assertEquals("table row NULL-SHA, CRITICAL", where.get(Weakness.NULL_CIPHER));
        assertEquals("only the Kx column: RSA, 2 rows", where.get(Weakness.STATIC_RSA));
        assertEquals("only the Kx column: DH 1024", where.get(Weakness.SMALL_DH));
    }

    @Test
    void aTableColumnIsPaddedAndNamesAreShort() {
        assertEquals("weak    found", TlsAttacksDemo.col("weak", 8, "found"));
        assertEquals("a-long-key found", TlsAttacksDemo.col("a-long-key", 4, "found"));
        assertEquals("TLS 1.0, DH 1024 bits", TlsAttacksDemo.names(EnumSet.of(Weakness.TLS_1_0, Weakness.SMALL_DH)));
    }

    @Test
    void theStrongServerShowsNone() {
        List<TestSsl.Finding> findings = TestSsl.parse(STRONG_REPORT);
        assertTrue(TestSsl.detect(findings).isEmpty());
        assertEquals(0, TestSsl.atLeastLow(findings));
    }

    @Test
    void theTruthListIsSix() {
        assertEquals(6, Weakness.PLANTED.size());
        Set<Weakness> found = EnumSet.of(Weakness.TLS_1_0);
        assertEquals(5, TlsAttacksDemo.missed(found).size());
        assertEquals("none", TlsAttacksDemo.names(EnumSet.noneOf(Weakness.class)));
    }

    @Test
    void anS_clientOutputIsParsed() {
        Probe ok = Probe.parse("""
            CONNECTION ESTABLISHED
            Protocol version: TLSv1.2
            Ciphersuite: DHE-RSA-AES128-GCM-SHA256
            Peer Temp Key: DH, 1024 bits
            """);
        assertTrue(ok.connected());
        assertEquals("TLSv1.2", ok.protocol());
        assertEquals("DHE-RSA-AES128-GCM-SHA256", ok.cipher());
        assertEquals(1024, ok.dhBits());
        Probe refused = Probe.parse("X:error:0A00043E:SSL routines:ssl3_read_bytes:tlsv1 alert inappropriate fallback:ssl/record/rec_layer_s3.c:918:SSL alert number 86");
        assertFalse(refused.connected());
        assertEquals("inappropriate fallback", refused.alert());
        assertEquals("TLS_AES_256_GCM_SHA384", Probe.parse("CONNECTION ESTABLISHED\nProtocol version: TLSv1.3\nCiphersuite: TLS_AES_256_GCM_SHA384\nNegotiated TLS1.3 group: x25519\n").cipher());
    }

    @Test
    void onlyThePinnedOpenSslSeriesIsAccepted() {
        assertTrue(Openssl.isPinned("OpenSSL 3.6.3 9 Jun 2026 (Library: OpenSSL 3.6.3 9 Jun 2026)"));
        assertFalse(Openssl.isPinned("OpenSSL 4.0.3 29 Sep 2026"));
        assertFalse(Openssl.isPinned("OpenSSL 3.5.0 8 Apr 2025"));
        assertFalse(Openssl.isPinned("LibreSSL 3.3.6"));
        assertFalse(Openssl.isPinned("OpenSSL 1.1.1w  11 Sep 2023"));
    }

    @Test
    void aPemIsBuiltFromPiecesAndWrapsTheBase64() {
        String pem = Lab.pem("PRIVATE KEY", new byte[100]);
        String dashes = "-".repeat(5);
        assertTrue(pem.startsWith(dashes + "BEGIN PRIVATE KEY" + dashes + "\n"));
        assertTrue(pem.endsWith(dashes + "END PRIVATE KEY" + dashes + "\n"));
        assertTrue(pem.lines().allMatch(l -> l.length() <= 64));
    }
}
