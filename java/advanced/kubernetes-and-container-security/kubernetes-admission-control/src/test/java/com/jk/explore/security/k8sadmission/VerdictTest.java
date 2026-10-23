package com.jk.explore.security.k8sadmission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VerdictTest {

    @Test
    void anExitCodeOfZeroIsAdmitted() {
        Verdict v = Verdict.of(new Sh.Result(0, "pod/p created (server dry run)"));
        assertTrue(v.admitted());
        assertEquals("admitted", v.toString());
    }

    @Test
    void everyPolicyThatFailedIsListedInOrder() {
        String msg = "Error from server: admission webhook \"vpol.validate.kyverno.svc-fail\" denied the request: Policy require-limits failed: x; Policy disallow-privileged failed: y";
        Verdict v = Verdict.of(new Sh.Result(1, msg));
        assertFalse(v.admitted());
        assertEquals(List.of("disallow-privileged", "require-limits"), v.policies());
        assertEquals("refused by disallow-privileged, require-limits", v.toString());
    }

    @Test
    void aWebhookThatCannotBeCalledIsNotAPolicyDecision() {
        Verdict v = Verdict.of(new Sh.Result(1, "Internal error occurred: failed calling webhook \"vpol.validate.kyverno.svc-fail\": no endpoints available for service \"kyverno-svc\""));
        assertTrue(v.webhookDown());
        assertEquals("refused: the webhook is unavailable", v.toString());
    }
}
