package com.jk.explore.security.containerhard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** How results are printed and how the capability mask is read, on fixed values: no Docker, no kind. */
class ReportTest {

    /** Docker's default fourteen capabilities, as /proc/self/status prints them. */
    static final String DOCKER_DEFAULT = "00000000a80425fb";

    private static Map<String, Boolean> results(String... possible) {
        Map<String, Boolean> r = new LinkedHashMap<>();
        for (String n : Probes.NAMES) {
            r.put(n, List.of(possible).contains(n));
        }
        return r;
    }

    @Test
    void dockersDefaultMaskHoldsItsFourteenCapabilitiesWithNetRawButNotSysAdmin() {
        assertEquals(List.of(0, 1, 3, 4, 5, 6, 7, 8, 10, 13, 18, 27, 29, 31), Capabilities.bits(DOCKER_DEFAULT));
        assertEquals(14, Capabilities.names(DOCKER_DEFAULT).size());
        assertTrue(Capabilities.held(DOCKER_DEFAULT, Capabilities.NET_RAW));
        assertFalse(Capabilities.held(DOCKER_DEFAULT, Capabilities.SYS_ADMIN));
        assertEquals(List.of(), Capabilities.bits("0000000000000000"));
        assertEquals(List.of("#40"), Capabilities.names("0000010000000000"));
    }

    @Test
    void theFactsAreReadFromTheScriptOutput() {
        Map<String, String> f = Probes.facts("read-secrets=POSSIBLE\nfact id-u=0\nfact CapEff=" + DOCKER_DEFAULT + "\nfact memory.max=max\n");
        assertEquals(Map.of("id-u", "0", "CapEff", DOCKER_DEFAULT, "memory.max", "max"), f);
        assertEquals(1, Probes.parse("read-secrets=POSSIBLE\nfact id-u=0\n").size());
    }

    @Test
    void theFactsAreExplained() {
        Report r = new Report();
        r.facts(Map.of("id-u", "10001", "CapEff", "0000000000000000", "Seccomp", "2", "memory.max", "536870912", "pids.max", "200"));
        assertEquals("""
                    id -u        10001 (not root)
                    CapEff       0000000000000000 (0 capabilities)
                    Seccomp      2 (a filter is on)
                    memory.max   536870912 (536870912 / 1048576 = 512 MiB)
                    pids.max     200 processes
                """, r.toString());
    }

    @Test
    void theCapabilityBitsAreWorkedOut() {
        Report r = new Report();
        r.capabilityBits(DOCKER_DEFAULT);
        String out = r.toString();
        assertTrue(out.contains("NET_RAW    (0xa80425fb >> 13) & 1 = 1, held"), out);
        assertTrue(out.contains("SYS_ADMIN  (0xa80425fb >> 21) & 1 = 0, not held"), out);
    }

    @Test
    void theCountAddsUpTheGroups() {
        Report r = new Report();
        r.count(results("read-secrets", "tamper-with-app", "drop-and-run", "become-root", "sniff-network", "exhaust-memory", "fork-bomb"));
        assertEquals("    container 4 + kernel 1 + host 0 + resources 2 = 7 of 13\n", r.toString());
    }

    @Test
    void aStepPrintsWhatItBlockedAndWhatItOpened() {
        Report r = new Report();
        r.step(4, "a user that is not root", results("read-secrets", "become-root"), results("read-secrets", "tamper-with-app"));
        assertEquals("""
                    4 a user that is not root          2 of 13
                        blocks become-root
                        opens  tamper-with-app
                """, r.toString());
    }

    @Test
    void theCommandIsWrittenOneOptionPerLine() {
        Report r = new Report();
        r.command(ContainerHardeningDemo.stage("limits", "<profile>"), "shop-app:probe");
        List<String> lines = r.toString().lines().toList();
        assertEquals("    docker run \\", lines.get(0));
        assertTrue(lines.contains("      --read-only \\"));
        assertTrue(lines.contains("      --tmpfs /tmp:rw,noexec,nosuid \\"));
        assertTrue(lines.contains("      --security-opt seccomp=<profile> \\"));
        assertEquals("      shop-app:probe", lines.get(lines.size() - 1));
    }

    @Test
    void everyAttemptHasAReasonEitherWayAndEveryLineFits() {
        Report r = new Report();
        r.attempts(results(Probes.NAMES));
        r.attempts(results());
        r.wrap("    blocks ", List.of(Probes.NAMES));
        assertEquals(13, Probes.REACH.size());
        assertTrue(r.toString().lines().allMatch(l -> l.length() <= Report.WIDTH), r.toString());
    }

    @Test
    void theSeccompProfileDeniesThirtyFiveSystemCalls() throws Exception {
        assertEquals(35, Seccomp.denied());
    }
}
