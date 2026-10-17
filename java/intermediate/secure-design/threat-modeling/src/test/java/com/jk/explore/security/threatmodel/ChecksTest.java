package com.jk.explore.security.threatmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChecksTest {

    @Test
    void everyThreatHasADecisionAndEveryMitigationHasACheck() {
        Map<String, Decision> register = Register.bank();
        assertTrue(Register.undecided(StrideRules.generate(BankModel.baseline()), register).isEmpty());
        assertTrue(Register.unproven(register).isEmpty());
        assertEquals(20, register.size());
    }

    @Test
    void theBankAsBuiltPassesEveryCheck() {
        assertTrue(Checks.failures(BankModel.baseline()).isEmpty());
    }

    @Test
    void anUnencryptedRefundFlowFailsTheTlsCheckAndBringsTwoUndecidedThreats() {
        Model changed = BankModel.withRefunds();
        assertEquals(Map.of(Checks.TLS, List.of("F9 refund is not encrypted")), Checks.failures(changed));
        assertEquals(List.of("T:F9", "R:F9"),
                Register.undecided(StrideRules.generate(changed), Register.bank()).stream().map(Threat::key).toList());
    }

    @Test
    void theDraftFailsAuthenticationStoreAndOwnerChecks() {
        assertEquals(Set.of(Checks.MUTUAL_AUTH, Checks.STORE_PROTECTION, Checks.OWNERS), Checks.failures(BankModel.draft()).keySet());
    }

    @Test
    void aProcessThatTakesMoneyWithoutAuditingIsCaught() {
        Model sneaky = BankModel.baseline()
                .with(new Element("P5", "refund job", Kind.PROCESS, BankModel.INTERNAL, "payments team", false, Set.of("authorization")))
                .with(new Flow("F11", "P3", "P5", "refund order", true, true, true));
        assertEquals(List.of("P5 refund job does not write to the audit log"), Checks.failures(sneaky).get(Checks.AUDIT));
    }

    @Test
    void aMitigationThatNamesNoCheckIsUnproven() {
        Map<String, Decision> register = new java.util.LinkedHashMap<>(Register.bank());
        register.put("S:E1", Decision.mitigate("a policy document"));
        assertEquals(List.of("S:E1"), Register.unproven(register));
    }
}
