package com.jk.explore.security.dast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The two checks worked out by hand give the verdicts ZAP gives: raised on the flawed application, not on the fixed one. */
class ProbeTest {

    @Test
    void theScriptComesBackUnchangedOnlyFromTheFlawedApplication() throws Exception {
        try (BankApp flawed = new BankApp(false); BankApp fixed = new BankApp(true)) {
            assertTrue(Probe.reflected(Probe.get(flawed, "/search", "q", Probe.SCRIPT)));
            Probe.Answer escaped = Probe.get(fixed, "/search", "q", Probe.SCRIPT);
            assertFalse(Probe.reflected(escaped));
            assertTrue(escaped.content().contains("&lt;scrIpt&gt;"));
        }
    }

    @Test
    void theDatabaseWorksOutTheExpressionOnlyInTheFlawedApplication() throws Exception {
        try (BankApp flawed = new BankApp(false); BankApp fixed = new BankApp(true)) {
            assertTrue(Probe.computed(Probe.get(flawed, "/account", "id", "1"), Probe.get(flawed, "/account", "id", Probe.EXPRESSION), Probe.get(flawed, "/account", "id", "2")));
            assertFalse(Probe.computed(Probe.get(fixed, "/account", "id", "1"), Probe.get(fixed, "/account", "id", Probe.EXPRESSION), Probe.get(fixed, "/account", "id", "2")));
            assertEquals(400, Probe.get(fixed, "/account", "id", Probe.EXPRESSION).status());
        }
    }

    @Test
    void theRiskAndConfidenceCodesHaveNames() {
        Alert a = new Alert("x", 3, 2, java.util.List.of());
        assertEquals("high", a.riskName());
        assertEquals("medium", a.confidenceName());
    }
}
