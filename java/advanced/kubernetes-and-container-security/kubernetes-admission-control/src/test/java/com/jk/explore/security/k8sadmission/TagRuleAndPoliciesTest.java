package com.jk.explore.security.k8sadmission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TagRuleAndPoliciesTest {

    @Test
    void aVersionTagOrADigestIsAllowed() {
        assertTrue(TagRule.read("nginx:1.27").allowed());
        assertTrue(TagRule.read("registry:5000/bank/payments:1").allowed());
        assertTrue(TagRule.read("bank/payments@sha256:abc").allowed());
    }

    @Test
    void latestOrNoTagIsRefused() {
        assertFalse(TagRule.read("nginx:latest").allowed());
        TagRule port = TagRule.read("registry:5000/bank/payments");
        assertFalse(port.tag(), "the colon before the last slash is a port");
        assertFalse(port.allowed());
    }

    @Test
    void eachPolicyFileGivesItsNameActionAndMessage() throws Exception {
        assertEquals(new Policies.Info("signed-images-only", "Deny", "a valid signature with the bank's key"), Policies.read("signed-images"));
        assertEquals("Audit", Policies.read("readonly-rootfs").action());
        for (String f : KubernetesAdmissionControlDemo.DENY) {
            assertEquals("Deny", Policies.read(f).action(), f);
        }
    }

    @Test
    void aFlowMapIsWrittenOneFieldPerLine() {
        assertEquals(List.of("readOnlyRootFilesystem: true"), KubernetesAdmissionControlDemo.fields("{readOnlyRootFilesystem: true}"));
        assertEquals(List.of("allowPrivilegeEscalation: false", "readOnlyRootFilesystem: true"),
                KubernetesAdmissionControlDemo.fields("{\"allowPrivilegeEscalation\":false,\"readOnlyRootFilesystem\":true}"));
    }

    @Test
    void onlyTimeoutsAreTriedAgain() {
        assertTrue(Cluster.isTransient("failed to call webhook: Post \"https://kyverno-svc\": context deadline exceeded"));
        assertFalse(Cluster.isTransient("denied the request: Policy require-limits failed"));
        assertFalse(Cluster.isTransient("no endpoints available for service \"kyverno-svc\""));
    }

    @Test
    void aLongRefusalIsShortenedInTheTable() {
        assertEquals("admitted", KubernetesAdmissionControlDemo.answer(new Verdict(true, List.of(), false)));
        assertEquals("refused: require-limits", KubernetesAdmissionControlDemo.answer(new Verdict(false, List.of("require-limits"), false)));
        assertEquals("refused: 2 policies (below)", KubernetesAdmissionControlDemo.answer(new Verdict(false, List.of("a", "b"), false)));
    }
}
