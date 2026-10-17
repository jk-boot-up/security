package com.jk.explore.security.certs;

import java.time.Instant;
import java.time.LocalDate;

/** The online store's certificates, built fresh for each run: a root, an intermediate under it, and the server certificate for shop.example. */
final class Pki {

    static final String HOST = "shop.example";
    static final Instant TODAY = Instant.parse("2026-01-01T00:00:00Z");
    static final LocalDate START = LocalDate.of(2025, 1, 1);

    final Issued root;
    final Issued intermediate;
    final Issued server;

    Pki() {
        root = CertBuilder.issue(CertBuilder.Request.authority("Example Root CA", START, LocalDate.of(2035, 1, 1), 1, 1), CertBuilder.newKeys(), null);
        intermediate = CertBuilder.issue(CertBuilder.Request.authority("Example Issuing CA", START, LocalDate.of(2030, 1, 1), 2, 0), CertBuilder.newKeys(), root);
        server = CertBuilder.issue(CertBuilder.Request.server(HOST, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1), 3), CertBuilder.newKeys(), intermediate);
    }

    /** A certificate for the same name from the same issuer, whose validity ended before TODAY. */
    Issued expiredServer() {
        return CertBuilder.issue(CertBuilder.Request.server(HOST, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 4), CertBuilder.newKeys(), intermediate);
    }

    /** What an attacker's own authority could make: a certificate for shop.example that chains to a root nobody trusts. */
    static Issued[] rogue() {
        Issued rogueRoot = CertBuilder.issue(CertBuilder.Request.authority("Example Root CA", START, LocalDate.of(2035, 1, 1), 1, 1), CertBuilder.newKeys(), null);
        Issued rogueServer = CertBuilder.issue(CertBuilder.Request.server(HOST, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1), 9), CertBuilder.newKeys(), rogueRoot);
        return new Issued[] {rogueRoot, rogueServer};
    }
}
