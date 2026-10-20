package com.jk.explore.security.tlscheck;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Four fixed servers for the demo: a good one, a legacy one, one with a broken certificate, and one that only forgot its HTTP settings. */
public final class Profiles {

    public static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private static final HttpFacts GOOD_HTTP = new HttpFacts(63_072_000, true, true, true);
    private static final CertFacts GOOD_CERT = new CertFacts(List.of("shop.example", "*.shop.example"), NOW.minusSeconds(30L * 86400), NOW.plusSeconds(60L * 86400),
            "EC", 256, "SHA256withECDSA", false, true, true);

    private Profiles() {
    }

    public static ServerProfile good() {
        return new ServerProfile("shop.example", Set.of("TLSv1.2", "TLSv1.3"), Policy.INTERMEDIATE.cipherSuites(), true, false, true, GOOD_CERT, GOOD_HTTP);
    }

    public static ServerProfile legacy() {
        return new ServerProfile("legacy.shop.example", Set.of("TLSv1", "TLSv1.1", "TLSv1.2"),
                List.of("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
                false, true, false,
                new CertFacts(List.of("legacy.shop.example"), NOW.minusSeconds(900L * 86400), NOW.plusSeconds(200L * 86400), "RSA", 1024, "SHA1withRSA", false, true, true),
                new HttpFacts(86_400, false, false, true));
    }

    public static ServerProfile brokenCertificate() {
        return new ServerProfile("pay.shop.example", Set.of("TLSv1.2", "TLSv1.3"), Policy.INTERMEDIATE.cipherSuites(), true, false, true,
                new CertFacts(List.of("www.other.example"), NOW.minusSeconds(500L * 86400), NOW.minusSeconds(10L * 86400), "RSA", 2048, "SHA256withRSA", false, false, true),
                GOOD_HTTP);
    }

    public static ServerProfile forgotHttp() {
        return new ServerProfile("blog.shop.example", Set.of("TLSv1.2", "TLSv1.3"), Policy.INTERMEDIATE.cipherSuites(), true, false, true,
                new CertFacts(List.of("blog.shop.example"), NOW.minusSeconds(30L * 86400), NOW.plusSeconds(10L * 86400), "RSA", 2048, "SHA256withRSA", false, false, true),
                new HttpFacts(-1, false, false, false));
    }
}
