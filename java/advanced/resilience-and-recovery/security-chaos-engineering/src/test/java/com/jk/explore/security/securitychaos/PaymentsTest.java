package com.jk.explore.security.securitychaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The service against stub dependencies on this machine: no Docker, no proxy. A closed port stands in for a dead dependency. */
class PaymentsTest {

    private Backends backends;
    private Http http;
    private int deadPort;

    @BeforeEach
    void start() throws IOException {
        backends = new Backends("127.0.0.1");
        http = new Http(500);
        try (ServerSocket s = new ServerSocket(0)) {
            deadPort = s.getLocalPort();
        }
    }

    @AfterEach
    void stop() {
        http.close();
        backends.close();
    }

    private String live(String name) {
        return "http://127.0.0.1:" + backends.port(name);
    }

    private String dead() {
        return "http://127.0.0.1:" + deadPort;
    }

    private BankPayments service(boolean hardened, String authz, String fraud, String rate, String ocsp, AuditTrail audit) {
        return new BankPayments(new BankPayments.Endpoints(authz, fraud, live(Backends.AUDIT), rate, ocsp), hardened, http, audit);
    }

    private BankPayments allLive(boolean hardened, AuditTrail audit) {
        return service(hardened, live(Backends.AUTHZ), live(Backends.FRAUD), live(Backends.RATE), live(Backends.OCSP), audit);
    }

    private AuditTrail sink() {
        return new DroppingAudit(http, live(Backends.AUDIT));
    }

    @Test
    void healthyDependenciesGiveTheSameAnswerInBothVersions() {
        for (boolean hardened : new boolean[] {false, true}) {
            BankPayments s = allLive(hardened, sink());
            assertTrue(s.pay(new Payment("a" + hardened, "u1" + hardened, 100, "ok-1")).approved());
            assertFalse(s.pay(new Payment("b" + hardened, "u2" + hardened, 9000, "ok-1")).approved());
            assertFalse(s.pay(new Payment("c" + hardened, "u3" + hardened, 100, "rev-7")).approved());
            assertFalse(s.pay(new Payment("d" + hardened, "frozen" + hardened, 100, "ok-1")).approved());
        }
    }

    @Test
    void firstVersionFailsOpenWhenAuthorizationIsDead() {
        BankPayments s = service(false, dead(), live(Backends.FRAUD), live(Backends.RATE), live(Backends.OCSP), sink());
        assertTrue(s.pay(new Payment("a", "frozen-1", 100, "ok-1")).approved());
    }

    @Test
    void hardenedVersionFailsClosedWhenAuthorizationIsDead() {
        BankPayments s = service(true, dead(), live(Backends.FRAUD), live(Backends.RATE), live(Backends.OCSP), sink());
        assertEquals(Decision.Outcome.DENIED, s.pay(new Payment("a", "u1", 100, "ok-1")).outcome());
    }

    @Test
    void fraudOutageIsUnscreenedInTheFirstVersionAndReviewInTheHardenedOne() {
        Decision first = service(false, live(Backends.AUTHZ), dead(), live(Backends.RATE), live(Backends.OCSP), sink())
                .pay(new Payment("a", "u1", 100, "ok-1"));
        assertTrue(first.approved());
        assertFalse(first.screened());
        Decision hardened = service(true, live(Backends.AUTHZ), dead(), live(Backends.RATE), live(Backends.OCSP), sink())
                .pay(new Payment("b", "u2", 100, "ok-1"));
        assertEquals(Decision.Outcome.REVIEW, hardened.outcome());
    }

    @Test
    void rateLimitFallsBackToACountInTheProcess() {
        BankPayments first = service(false, live(Backends.AUTHZ), live(Backends.FRAUD), dead(), live(Backends.OCSP), sink());
        BankPayments hardened = service(true, live(Backends.AUTHZ), live(Backends.FRAUD), dead(), live(Backends.OCSP), sink());
        int a = 0;
        int b = 0;
        for (int i = 0; i < 12; i++) {
            a += first.pay(new Payment("f" + i, "burst", 10, "ok-1")).approved() ? 1 : 0;
            b += hardened.pay(new Payment("h" + i, "burst", 10, "ok-1")).approved() ? 1 : 0;
        }
        assertEquals(12, a);
        assertEquals(BankPayments.LIMIT, b);
    }

    @Test
    void revocationSoftFailsInTheFirstVersionAndHardFailsInTheHardenedOne() {
        BankPayments first = service(false, live(Backends.AUTHZ), live(Backends.FRAUD), live(Backends.RATE), dead(), sink());
        BankPayments hardened = service(true, live(Backends.AUTHZ), live(Backends.FRAUD), live(Backends.RATE), dead(), sink());
        assertTrue(first.pay(new Payment("a", "u1", 100, "rev-7")).approved());
        assertFalse(hardened.pay(new Payment("b", "u2", 100, "rev-7")).approved());
        assertFalse(hardened.pay(new Payment("c", "u3", 100, "never-seen")).approved());
    }

    @Test
    void aFreshRevocationAnswerBeatsTheCache() {
        AuditTrail audit = sink();
        BankPayments warm = allLive(true, audit);
        assertTrue(warm.pay(new Payment("a", "u1", 100, "ok-2")).approved());
        backends.revoke("ok-2");
        assertFalse(warm.pay(new Payment("b", "u2", 100, "ok-2")).approved());
    }

    @Test
    void droppingAuditLosesEverythingWhileTheSinkIsDead() {
        DroppingAudit audit = new DroppingAudit(http, dead());
        for (int i = 0; i < 10; i++) {
            assertTrue(audit.record("r" + i));
        }
        audit.flush();
        assertEquals(10, audit.dropped());
        assertEquals(0, backends.auditCount());
    }

    @Test
    void spooledAuditKeepsRecordsAndSendsThemWhenTheSinkIsBack() throws IOException {
        Path dir = Files.createTempDirectory("sc-test");
        try {
            // with a dead sink the records wait and are journaled; with a live sink flush leaves nothing waiting
            SpooledAudit audit = new SpooledAudit(http, dead(), dir.resolve("j.log"));
            for (int i = 0; i < 7; i++) {
                assertTrue(audit.record("r" + i));
            }
            assertEquals(7, audit.waiting());
            assertEquals(7, Files.readAllLines(dir.resolve("j.log")).size());
            SpooledAudit live = new SpooledAudit(http, live(Backends.AUDIT), dir.resolve("k.log"));
            for (int i = 0; i < 7; i++) {
                live.record("r" + i);
            }
            live.flush();
            assertEquals(0, live.waiting());
            assertEquals(7, backends.auditCount());
        } finally {
            for (String f : new String[] {"j.log", "k.log"}) {
                Files.deleteIfExists(dir.resolve(f));
            }
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void anAuditTrailThatCannotBeWrittenRefusesThePayment() {
        SpooledAudit audit = new SpooledAudit(http, live(Backends.AUDIT), Path.of("no-such-dir-for-sure", "j.log"));
        BankPayments s = allLive(true, audit);
        Decision d = s.pay(new Payment("a", "u1", 100, "ok-1"));
        assertEquals(Decision.Outcome.DENIED, d.outcome());
    }

    @Test
    void aStalledAnswerCannotKeepTheClientWaitingPastTheDeadline() throws IOException {
        try (ServerSocket silent = new ServerSocket(0); Http quick = new Http(300)) {
            long start = System.nanoTime();
            assertTrue(quick.get("http://127.0.0.1:" + silent.getLocalPort() + "/x").isEmpty());
            assertTrue((System.nanoTime() - start) / 1_000_000 < 3000);
        }
    }
}
