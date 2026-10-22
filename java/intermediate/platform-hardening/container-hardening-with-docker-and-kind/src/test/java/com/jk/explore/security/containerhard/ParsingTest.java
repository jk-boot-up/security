package com.jk.explore.security.containerhard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The readers of the probe output and of the admission messages, on text. */
class ParsingTest {

    @Test
    void theProbeOutputIsReadAsAttackNamesAndResults() {
        Map<String, Boolean> r = Probes.parse("read-secrets=POSSIBLE\nsome noise\nmount-host=blocked\n");
        assertEquals(Map.of("read-secrets", true, "mount-host", false), r);
        assertEquals(1, Probes.possible(r));
        assertEquals(List.of("read-secrets"), Probes.possibleNames(r));
    }

    @Test
    void thereAreThirteenNamedAttacksAndTheScriptMentionsEachOfThem() {
        assertEquals(13, Probes.NAMES.length);
        for (String name : Probes.NAMES) {
            assertEquals(true, Probes.SCRIPT.contains("N=" + name + ";"), name);
        }
    }

    @Test
    void aRefusalIsSplitIntoItsViolationsEvenWhenTheyHaveCommasInside() {
        String message = "Error from server (Forbidden): pods \"x\" is forbidden: violates PodSecurity \"restricted:latest\": host namespaces (hostNetwork=true, hostPID=true), privileged (container \"app\" must not set"
                + " securityContext.privileged=true), allowPrivilegeEscalation != false (container \"app\" must set securityContext.allowPrivilegeEscalation=false), seccompProfile (pod or container \"app\" must set x)";
        assertEquals(List.of("host namespaces", "privileged", "allowPrivilegeEscalation != false", "seccompProfile"), Cluster.violations(message));
    }

    @Test
    void aWarningIsReadTheSameWay() {
        assertEquals(List.of("runAsNonRoot != true", "seccompProfile"), Cluster.violations("Warning: would violate PodSecurity \"restricted:latest\": runAsNonRoot != true (pod must set it), seccompProfile (must set it)"));
        assertEquals(List.of(), Cluster.violations("pod/plain created"));
    }

    @Test
    void theStagesAddOneOptionAtATime() {
        assertEquals(List.of("--security-opt", "seccomp=p"), ContainerHardeningDemo.stage("default", "p"));
        assertEquals(List.of("--security-opt", "seccomp=p", "--cap-drop", "ALL"), ContainerHardeningDemo.stage("no-caps", "p"));
        List<String> last = ContainerHardeningDemo.stage("limits", "p");
        assertEquals(true, last.containsAll(List.of("--user", "10001", "--read-only", "--memory", "512m", "--pids-limit", "200", "no-new-privileges")));
    }

    @Test
    void theManifestsAskForWhatTheirNamesSay() {
        assertEquals(true, Pods.tutorial("t").contains("privileged: true") && Pods.tutorial("t").contains("hostNetwork: true"));
        assertEquals(false, Pods.plain("p").contains("securityContext"));
        assertEquals(true, Pods.hardened("h").contains("readOnlyRootFilesystem: true") && Pods.hardened("h").contains("drop: [ALL]") && Pods.hardened("h").contains("RuntimeDefault"));
    }
}
