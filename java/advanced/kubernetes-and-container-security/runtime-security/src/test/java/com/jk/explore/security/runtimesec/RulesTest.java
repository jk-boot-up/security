package com.jk.explore.security.runtimesec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RulesTest {

    static final Map<String, String> PODS = Map.of(AuditTest.CID, "payments-api");
    static final Rules.Baseline BASELINE = new Rules.Baseline(List.of("/var/log/payments/", "/tmp/", "/dev/"), Set.of("10.96.1.1", "10.96.0.10"));

    static Event event(String key, String comm, boolean ok, String container, List<String> paths, String address) {
        return new Event("1", key, comm, ok, container, paths, address);
    }

    Set<Alert> run(Event... events) {
        return Rules.evaluate(List.of(events), PODS, BASELINE);
    }

    @Test
    void aShellStartedInAContainerIsReported() {
        for (String shell : List.of("sh", "bash", "ash", "dash", "zsh")) {
            assertEquals(Set.of(new Alert(Rules.SHELL, "payments-api", shell)), run(event("exec", shell, true, AuditTest.CID, List.of(), null)));
        }
    }

    @Test
    void anotherProgramIsNotAShell() {
        assertTrue(run(event("exec", "wget", true, AuditTest.CID, List.of(), null)).isEmpty());
        assertTrue(run(event("exec", "shred", true, AuditTest.CID, List.of(), null)).isEmpty());
    }

    @Test
    void aShellThatFailedToStartIsNotReported() {
        assertTrue(run(event("exec", "sh", false, AuditTest.CID, List.of(), null)).isEmpty());
    }

    @Test
    void aWriteOutsideTheExpectedFoldersIsReported() {
        assertEquals(Set.of(new Alert(Rules.WRITE, "payments-api", "/usr/bin/miner")),
                run(event("write", "tee", true, AuditTest.CID, List.of("/usr/bin/miner"), null)));
        assertEquals(1, run(event("write", "tee", true, AuditTest.CID, List.of("/etc/passwd"), null)).size());
    }

    @Test
    void aWriteInTheLogFolderOrTmpIsExpected() {
        assertTrue(run(event("write", "tee", true, AuditTest.CID, List.of("/var/log/payments/audit.log"), null)).isEmpty());
        assertTrue(run(event("write", "tee", true, AuditTest.CID, List.of("/tmp/x"), null)).isEmpty());
        assertTrue(run(event("write", "tee", true, AuditTest.CID, List.of("/dev/null"), null)).isEmpty());
    }

    @Test
    void aFolderNamedLikeTheLogFolderIsNotTheLogFolder() {
        assertEquals(1, run(event("write", "tee", true, AuditTest.CID, List.of("/var/log/payments-old/x"), null)).size());
    }

    @Test
    void aRefusedWriteIsStillReportedAndMarked() {
        assertEquals(Set.of(new Alert(Rules.WRITE, "payments-api", "/usr/bin/ (refused)")),
                run(event("write", "tee", false, AuditTest.CID, List.of("/usr/bin/"), null)));
    }

    @Test
    void aConnectionToAnUnknownAddressIsReported() {
        assertEquals(Set.of(new Alert(Rules.CONNECT, "payments-api", "172.18.0.9:4444")),
                run(event("connect", "nc", false, AuditTest.CID, List.of(), "172.18.0.9:4444")));
    }

    @Test
    void theLedgerTheDnsAndTheLoopbackAreExpected() {
        assertTrue(run(event("connect", "nc", true, AuditTest.CID, List.of(), "10.96.1.1:5432")).isEmpty());
        assertTrue(run(event("connect", "nc", true, AuditTest.CID, List.of(), "10.96.0.10:53")).isEmpty());
        assertTrue(run(event("connect", "wget", true, AuditTest.CID, List.of(), "127.0.0.1:8080")).isEmpty());
    }

    @Test
    void aConnectionWithoutAnIpv4AddressIsNotJudged() {
        assertTrue(run(event("connect", "nc", true, AuditTest.CID, List.of(), null)).isEmpty());
    }

    @Test
    void whatRunsOutsideTheKnownPodsIsIgnored() {
        assertTrue(run(event("exec", "sh", true, "b".repeat(64), List.of(), null), event("exec", "sh", true, null, List.of(), null)).isEmpty());
    }

    @Test
    void theSameBehaviourIsReportedOnce() {
        Event e = event("exec", "sh", true, AuditTest.CID, List.of(), null);
        assertEquals(1, run(e, e, e).size());
    }
}
