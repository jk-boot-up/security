package com.jk.explore.security.certsbc;

import java.time.Instant;
import java.time.LocalDate;

/** The online store's certificates, built fresh for each run: a root, an intermediate limited to the store's names, and the server certificate for shop.example. */
final class Pki {

    static final String HOST = "shop.example";
    static final Instant TODAY = Instant.parse("2026-01-01T00:00:00Z");
    static final LocalDate START = LocalDate.of(2025, 1, 1);

    final Issued root;
    final Issued intermediate;
    final Issued server;

    Pki() {
        root = CertFactory.selfSigned(CertFactory.Spec.authority("Example Root CA", START, LocalDate.of(2035, 1, 1), 1, 1), CertFactory.newKeys());
        intermediate = CertFactory.issue(CertFactory.Spec.authority("Example Issuing CA", START, LocalDate.of(2030, 1, 1), 2, 0).limitedTo(HOST),
                CertFactory.newKeys(), root);
        server = CertFactory.issue(CertFactory.Spec.server(HOST, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1), 3), CertFactory.newKeys(), intermediate);
    }

    /** A certificate from the same intermediate for another name, which the intermediate's own limit forbids. */
    Issued serverFor(String host, long serial) {
        return CertFactory.issue(CertFactory.Spec.server(host, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1), serial), CertFactory.newKeys(), intermediate);
    }

    /** A certificate for the same name from the same issuer, whose validity ended before TODAY. */
    Issued expiredServer() {
        return CertFactory.issue(CertFactory.Spec.server(HOST, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 4), CertFactory.newKeys(), intermediate);
    }

    /** What an attacker's own authority could make: a certificate for shop.example that chains to a root nobody trusts, with the real root's name. */
    static Issued[] rogue() {
        Issued rogueRoot = CertFactory.selfSigned(CertFactory.Spec.authority("Example Root CA", START, LocalDate.of(2035, 1, 1), 1, 1), CertFactory.newKeys());
        Issued rogueServer = CertFactory.issue(CertFactory.Spec.server(HOST, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1), 9), CertFactory.newKeys(), rogueRoot);
        return new Issued[] {rogueRoot, rogueServer};
    }
}
