package com.jk.explore.security.pytmmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The design and the generated Python, with no pytm. */
class WriterTest {

    @Test
    void theBankHasSevenElementsEightFlowsAndFourBoundaries() {
        Design bank = Design.bank();
        assertEquals(7, bank.elements().size());
        assertEquals(8, bank.flows().size());
        assertEquals(4, bank.boundaries().size());
    }

    @Test
    void theModelDeclaresBoundariesElementsFlowsAndProcessesTheModel() {
        String python = Writer.python(Design.bank(), Answers.Stage.NONE, "Bank payments");
        assertTrue(python.startsWith("from pytm import"));
        assertTrue(python.contains("tm = TM(\"Bank payments\")"));
        assertTrue(python.contains("gateway = Server(\"API gateway\")"));
        assertTrue(python.contains("customer = Actor(\"Customer\")"));
        assertTrue(python.contains("accounts = Datastore(\"Accounts database\")"));
        assertTrue(python.contains("f2 = Dataflow(app, gateway, \"Payment request with token\")"));
        assertTrue(python.trim().endsWith("tm.process()"));
    }

    @Test
    void noAnswersMeansNoControlsAreSet() {
        assertFalse(Writer.python(Design.bank(), Answers.Stage.NONE, "x").contains(".controls."));
    }

    @Test
    void eachStageAddsAnswersAndKeepsTheEarlierOnes() {
        int previous = 0;
        for (Answers.Stage stage : Answers.Stage.values()) {
            String python = Writer.python(Design.bank(), stage, "x");
            long controls = python.lines().filter(l -> l.contains(".controls.")).count();
            assertTrue(controls >= previous, stage + " has " + controls + " after " + previous);
            previous = (int) controls;
        }
        assertTrue(Writer.python(Design.bank(), Answers.Stage.RESILIENCE, "x").contains("f2.controls.isEncrypted = True"));
        assertTrue(Writer.python(Design.bank(), Answers.Stage.RESILIENCE, "x").contains("gateway.controls.isHardened = True"));
    }

    @Test
    void theDraftAddsACardVaultAndAFlowAndTheRefundsAddAFlow() {
        assertEquals(8, Design.draft().elements().size());
        assertEquals(9, Design.draft().flows().size());
        assertEquals(9, Design.withRefunds().flows().size());
        assertFalse(Writer.python(Design.withRefunds(), Answers.Stage.RESILIENCE, "x").contains("f9.controls.isEncrypted"));
    }
}
