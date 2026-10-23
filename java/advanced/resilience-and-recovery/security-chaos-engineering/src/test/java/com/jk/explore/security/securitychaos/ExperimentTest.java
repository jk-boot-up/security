package com.jk.explore.security.securitychaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
