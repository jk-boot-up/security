package com.jk.explore.security.k8sadmission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PodsAndPoliciesTest {

    @Test
    void aGoodPodHasLimitsAndAReadOnlyRoot() {
        String yaml = Pods.pod(Pods.ok("r/x@sha256:1"));
        assertTrue(yaml.contains("limits: {cpu: 500m, memory: 256Mi}"));
        assertTrue(yaml.contains("readOnlyRootFilesystem: true"));
        assertFalse(yaml.contains("privileged"));
    }

    @Test
    void aPrivilegedPodWithNoLimitsSaysSo() {
        String yaml = Pods.pod(new Pods.Spec("p", "nginx:latest", true, false, false));
        assertTrue(yaml.contains("privileged: true"));
        assertFalse(yaml.contains("limits"));
    }

    @Test
    void aDeploymentCarriesTheContainerInItsTemplate() {
        String yaml = Pods.deployment(new Pods.Spec("p", "r/x:1", true, true, true));
        assertTrue(yaml.contains("kind: Deployment"));
        assertTrue(yaml.contains("        - name: app"));
    }

    @Test
    void theNameCanBeChanged() {
        assertEquals("other", Pods.ok("i").withName("other").name());
    }

    @Test
    void policyFilesMapToTheirKinds() {
        assertEquals("imagevalidatingpolicy", Kyverno.kindOf("signed-images"));
        assertEquals("mutatingpolicy", Kyverno.kindOf("default-no-escalation"));
        assertEquals("validatingpolicy", Kyverno.kindOf("require-limits"));
        assertEquals("signed-images-only", Kyverno.policyName("signed-images"));
        assertEquals("require-limits", Kyverno.policyName("require-limits"));
    }

    @Test
    void everyPolicyFileIsScopedToLabelledNamespaces() throws Exception {
        for (String f : List.of("disallow-privileged", "require-limits", "no-latest-tag", "trusted-registry", "readonly-rootfs", "default-no-escalation", "signed-images")) {
            String text = Files.readString(Kyverno.POLICIES.resolve(f + ".yaml"));
            assertTrue(text.contains("bank/admission: enforced"), f);
        }
    }

    @Test
    void theRulesRefuseByDefaultExceptTheOneInAuditMode() throws Exception {
        for (String f : List.of("disallow-privileged", "require-limits", "no-latest-tag", "trusted-registry", "signed-images")) {
            assertTrue(Files.readString(Kyverno.POLICIES.resolve(f + ".yaml")).contains("validationActions: [Deny]"), f);
        }
        assertTrue(Files.readString(Path.of("policies", "readonly-rootfs.yaml")).contains("validationActions: [Audit]"));
    }

    @Test
    void theExceptionHasAnExpiryAndLivesInTheKyvernoNamespace() throws Exception {
        String text = Files.readString(Kyverno.POLICIES.resolve("exception.yaml"));
        assertTrue(text.contains("expiresAt"));
        assertTrue(text.contains("namespace: kyverno"));
    }
}
