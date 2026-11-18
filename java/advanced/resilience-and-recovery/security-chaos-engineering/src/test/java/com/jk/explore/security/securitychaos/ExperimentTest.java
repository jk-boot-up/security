package com.jk.explore.security.securitychaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExperimentTest {

    @Test
    void tenNumberedExperimentsOverFiveDependencies() {
        List<Experiment> all = Experiment.all();
        assertEquals(10, all.size());
        assertEquals(10, all.stream().map(Experiment::number).distinct().count());
        assertEquals(5, all.stream().map(Experiment::dependency).distinct().count());
    }

    @Test
    void everyFaultButDownIsAToxicWithJson() {
        assertNull(Fault.DOWN.toxicJson());
        for (Fault f : Fault.values()) {
            if (f != Fault.DOWN) {
                assertNotNull(f.toxicJson());
                assertTrue(f.toxicJson().contains("\"type\""));
            }
        }
    }

    @Test
    void onlyTheSlowCloseExperimentExpectsNoAlert() {
        List<Experiment> quiet = Experiment.all().stream().filter(e -> !e.alertExpected()).toList();
        assertEquals(1, quiet.size());
        assertEquals(Fault.SLOW_CLOSE, quiet.getFirst().fault());
    }

    @Test
    void queryParsing() {
        assertEquals("b", Backends.query("a=1&x=b").get("x"));
    }

    @Test
    void aTrickledAnswerTakesLongerThanTheDeadline() {
        int bytes = Backends.bodyLength("score=10");
        assertEquals(4009, bytes);
        assertTrue(bytes * 1000L / Fault.TRICKLE_BYTES_PER_SECOND > Lab.DEADLINE_MS);
    }

    @Test
    void faultLabelsAreShortAndDistinct() {
        assertEquals(Fault.values().length, java.util.Arrays.stream(Fault.values()).map(Fault::label).distinct().count());
        for (Fault f : Fault.values()) {
            assertTrue(f.label().length() <= 11);
        }
    }

    @Test
    void theSteadyStateNeedsTwoApprovalsTwoRefusalsAndFourRecords() {
        List<Decision> right = List.of(Decision.approve(true, "screened"), Decision.approve(true, "screened"),
                Decision.deny("fraud score"), Decision.deny("certificate"));
        assertTrue(Lab.steadyHolds(right, 4));
        assertFalse(Lab.steadyHolds(right, 3));
        assertFalse(Lab.steadyHolds(List.of(right.get(0), right.get(1), right.get(0), right.get(3)), 4));
    }
}
