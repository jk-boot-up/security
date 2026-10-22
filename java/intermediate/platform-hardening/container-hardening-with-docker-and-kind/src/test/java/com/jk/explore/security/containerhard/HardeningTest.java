package com.jk.explore.security.containerhard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Real containers and a real kind cluster. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HardeningTest {

    private String seccomp;
    private Cluster cluster;

    @BeforeAll
    void start() throws Exception {
        Image.build();
        seccomp = Seccomp.profile().toString();
        cluster = new Cluster();
        cluster.namespace("t-baseline", "baseline", "restricted");
        cluster.namespace("t-restricted", "restricted", "restricted");
    }

    @AfterAll
    void stop() {
        cluster.close();
    }

    private Map<String, Boolean> docker(String stage) throws Exception {
        return Run.probe(ContainerHardeningDemo.stage(stage, seccomp));
    }

    @Test
    void theTutorialConfigurationLetsAllThirteenThrough() throws Exception {
        assertEquals(13, Probes.possible(docker("tutorial")));
    }

    @Test
    void eachStageNeverMakesThingsWorse() throws Exception {
        long previous = 13;
        for (String stage : List.of("tutorial", "unprivileged", "default", "no-caps", "non-root", "no-new-privileges", "read-only", "limits")) {
            long now = Probes.possible(docker(stage));
            assertTrue(now <= previous, stage + ": " + now + " after " + previous);
            previous = now;
        }
        assertEquals(1, previous);
    }

    @Test
    void theHardenedContainerCanStillReadItsSecrets() throws Exception {
        assertEquals(List.of("read-secrets"), Probes.possibleNames(docker("limits")));
    }

    @Test
    void droppingCapabilitiesTakesAwayRawSocketsAndMountButNotRoot() throws Exception {
        Map<String, Boolean> r = docker("no-caps");
        assertFalse(r.get("sniff-network"));
        assertFalse(r.get("mount-host"));
        assertTrue(r.get("become-root"));
    }

    @Test
    void runningAsAnotherUserTakesAwayRootAndNoNewPrivilegesChangesNothingInAnImageWithoutSetuidFiles() throws Exception {
        assertTrue(docker("no-caps").get("become-root"));
        assertFalse(docker("non-root").get("become-root"));
        assertEquals(Probes.possible(docker("non-root")), Probes.possible(docker("no-new-privileges")));
    }

    @Test
    void theSeccompProfileIsTheOnlyThingBetweenRootWithSysAdminAndMount() throws Exception {
        List<String> options = new java.util.ArrayList<>(ContainerHardeningDemo.stage("limits", seccomp));
        options.addAll(List.of("--user", "0", "--cap-add", "SYS_ADMIN"));
        assertFalse(Run.probe(options).get("mount-host"));
        options.set(options.indexOf("seccomp=" + seccomp), "seccomp=unconfined");
        assertTrue(Run.probe(options).get("mount-host"));
    }

    @Test
    void restrictedRefusesAPlainPodAndAdmitsTheHardenedOne() throws Exception {
        assertFalse(cluster.apply("t-restricted", Pods.plain("p"), "--dry-run=server").ok());
        assertEquals(4, Cluster.violations(cluster.apply("t-restricted", Pods.plain("p"), "--dry-run=server").output()).size());
        assertTrue(cluster.apply("t-restricted", Pods.hardened("h"), "--dry-run=server").ok());
        assertTrue(cluster.apply("t-baseline", Pods.plain("p"), "--dry-run=server").ok());
        assertFalse(cluster.apply("t-baseline", Pods.tutorial("t"), "--dry-run=server").ok());
    }

    @Test
    void theHardenedPodHasTwoAttacksLeftAndThePlainPodSeven() throws Exception {
        cluster.apply("t-baseline", Pods.plain("plain"));
        cluster.apply("t-restricted", Pods.hardened("hardened"));
        assertEquals(7, Probes.possible(cluster.probe("t-baseline", "plain")));
        assertEquals(List.of("read-secrets", "drop-and-run"), Probes.possibleNames(cluster.probe("t-restricted", "hardened")));
    }
}
