package com.jk.explore.security.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContainerTest {

    private static boolean possible(ContainerSpec c, String id) {
        return Attacker.run(c).stream().filter(o -> o.attack().id().equals(id)).findFirst().orElseThrow().possible();
    }

    @Test
    void thereAreThirteenAttacksAndTheyAreAllEvaluated() {
        assertEquals(13, Attacker.ATTACKS.size());
        assertEquals(13, Attacker.run(ContainerSpec.dockerDefaults()).size());
    }

    @Test
    void dockerDefaultsAreRootWithCapabilitiesAndNoLimits() {
        ContainerSpec c = ContainerSpec.dockerDefaults();
        assertEquals(7, Attacker.possible(Attacker.run(c)));
        assertTrue(possible(c, "become-root") && possible(c, "sniff-network") && possible(c, "exhaust-memory") && possible(c, "fork-bomb"));
        assertFalse(possible(c, "mount-host") || possible(c, "docker-socket") || possible(c, "dangerous-syscalls"));
        assertEquals(PodSecurityLevel.BASELINE, PodSecurityLevel.of(c));
    }

    @Test
    void theRecklessConfigurationAllowsEverythingAndIsPrivileged() {
        ContainerSpec c = ContainerSpec.reckless();
        assertEquals(13, Attacker.possible(Attacker.run(c)));
        assertEquals(PodSecurityLevel.PRIVILEGED, PodSecurityLevel.of(c));
        assertEquals(6, PodSecurityLevel.violations(c, PodSecurityLevel.BASELINE).size());
    }

    @Test
    void theDockerSocketMeansRootOnTheHostWhateverElseIsHardened() {
        ContainerSpec c = ContainerDemo.hardened();
        assertFalse(possible(c, "docker-socket"));
        ContainerSpec withSocket = new ContainerSpec(c.name(), c.user(), false, c.capabilities(), true, true, false, Seccomp.RUNTIME_DEFAULT, List.of("/var/run/docker.sock"), false, false,
                512, 200, true, false, c.image());
        assertTrue(possible(withSocket, "docker-socket"));
        assertEquals(PodSecurityLevel.PRIVILEGED, PodSecurityLevel.of(withSocket));
    }

    @Test
    void eachSettingBlocksItsAttack() {
        ContainerSpec base = ContainerSpec.dockerDefaults();
        assertFalse(possible(base.readOnlyFilesystem(), "tamper-with-app"));
        assertFalse(possible(base.readOnlyFilesystem(), "drop-and-run"));
        assertFalse(possible(base.dropAllCapabilities(), "sniff-network"));
        assertFalse(possible(base.withLimits(256, 100), "exhaust-memory"));
        assertFalse(possible(base.withLimits(256, 100), "fork-bomb"));
        assertFalse(possible(base.user("10001").noNewPrivileges(), "become-root"));
        assertTrue(possible(base.user("10001"), "become-root"), "a non-root user can still use set-user-id programs");
        assertTrue(possible(base.withSeccomp(Seccomp.UNCONFINED), "dangerous-syscalls"));
    }

    @Test
    void aReadOnlyFilesystemWithAnExecutableTmpStillAllowsDroppingATool() {
        ContainerSpec c = new ContainerSpec("x", "10001", false, java.util.Set.of(), true, false, false, Seccomp.RUNTIME_DEFAULT, List.of(), false, false, 512, 200, true, false, "i");
        assertTrue(possible(c, "drop-and-run"));
        assertFalse(possible(c, "tamper-with-app"));
    }

    @Test
    void rootWithSysAdminCanMountButNonRootWithItCannot() {
        ContainerSpec hardened = ContainerDemo.hardened();
        assertTrue(possible(hardened.user("root").addCapability("SYS_ADMIN"), "mount-host"));
        assertFalse(possible(hardened.addCapability("SYS_ADMIN"), "mount-host"));
        assertFalse(possible(hardened.addCapability("NET_RAW"), "sniff-network"));
        assertTrue(possible(hardened.user("root").addCapability("NET_RAW"), "sniff-network"));
    }

    @Test
    void theHardenedContainerLeavesOnlyTheSecretsItNeeds() {
        List<Outcome> o = Attacker.run(ContainerDemo.hardened());
        assertEquals(1, Attacker.possible(o));
        assertEquals("read-secrets", o.stream().filter(Outcome::possible).findFirst().orElseThrow().attack().id());
        assertEquals(PodSecurityLevel.RESTRICTED, PodSecurityLevel.of(ContainerDemo.hardened()));
        assertTrue(possible(ContainerDemo.hardened(), "read-secrets"));
    }

    @Test
    void podSecurityLevelsNestAndNameTheirViolations() {
        ContainerSpec d = ContainerSpec.dockerDefaults();
        assertEquals(List.of(), PodSecurityLevel.violations(d, PodSecurityLevel.BASELINE));
        assertEquals(List.of("runs as root", "allowPrivilegeEscalation is true", "does not drop ALL capabilities"), PodSecurityLevel.violations(d, PodSecurityLevel.RESTRICTED));
        assertEquals(List.of(), PodSecurityLevel.violations(ContainerSpec.reckless(), PodSecurityLevel.PRIVILEGED));
        assertTrue(PodSecurityLevel.violations(d.addCapability("SYS_PTRACE"), PodSecurityLevel.BASELINE).contains("capability beyond the default set: SYS_PTRACE"));
    }

    @Test
    void theRenderedFlagsAndYamlMatchTheSpec() {
        String flags = Renderer.dockerFlags(ContainerDemo.hardened());
        assertTrue(flags.contains("--user 10001") && flags.contains("--cap-drop=ALL") && flags.contains("--read-only") && flags.contains("no-new-privileges")
                && flags.contains("--memory 512m") && flags.contains("--pids-limit 200") && flags.endsWith("shop-app@sha256:9f2c1a7d"));
        assertFalse(flags.contains("--privileged"));
        String yaml = Renderer.kubernetesSecurityContext(ContainerDemo.hardened());
        assertTrue(yaml.contains("runAsNonRoot: true") && yaml.contains("readOnlyRootFilesystem: true") && yaml.contains("drop: [ALL]") && yaml.contains("type: RuntimeDefault"));
        String reckless = Renderer.dockerFlags(ContainerSpec.reckless());
        assertTrue(reckless.contains("--privileged") && reckless.contains("--network host") && reckless.contains("-v /var/run/docker.sock:/var/run/docker.sock"));
    }
}
