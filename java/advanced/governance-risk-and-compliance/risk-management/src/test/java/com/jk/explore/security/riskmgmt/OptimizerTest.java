package com.jk.explore.security.riskmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OptimizerTest {

    private static final Risk BIG = new Risk("big", "big", 1, 1, new Tri(1, 1, 1), new Tri(1_000_000, 1_000_000, 1_000_000));

    private final Simulation sim = new Simulation(List.of(BIG), 5_000, 9);

    @Test
    void aControlThatCostsMoreThanItSavesIsNotChosen() {
        Control dear = new Control("dear", "dear", 900_000, Map.of("big", new Control.Effect(1, 0.9)));
        assertTrue(Optimizer.best(sim, List.of(dear), 2_000_000).controls().isEmpty());
    }

    @Test
    void aControlThatSavesMoreThanItCostsIsChosen() {
        Control good = new Control("good", "good", 100_000, Map.of("big", new Control.Effect(1, 0.5)));
        Optimizer.Choice c = Optimizer.best(sim, List.of(good), 2_000_000);
        assertEquals(List.of(good), c.controls());
        assertTrue(c.netBenefit() > 300_000);
    }

    @Test
    void theBudgetIsNotExceeded() {
        Control a = new Control("a", "a", 300_000, Map.of("big", new Control.Effect(1, 0.5)));
        Control b = new Control("b", "b", 300_000, Map.of("big", new Control.Effect(1, 0.5)));
        assertEquals(1, Optimizer.best(sim, List.of(a, b), 400_000).controls().size());
    }

    @Test
    void cheapestFirstCanLoseToTheBestSet() {
        Control cheapButWeak = new Control("weak", "weak", 100_000, Map.of("big", new Control.Effect(1, 0.95)));
        Control dearButStrong = new Control("strong", "strong", 500_000, Map.of("big", new Control.Effect(1, 0.1)));
        double best = Optimizer.best(sim, List.of(cheapButWeak, dearButStrong), 550_000).netBenefit();
        double cheap = Optimizer.cheapestFirst(sim, List.of(cheapButWeak, dearButStrong), 550_000).netBenefit();
        assertTrue(best >= cheap);
    }

    @Test
    void theRegisterHasEightRisksAndSevenControlsAndNoUnknownRiskIsTouched() {
        assertEquals(8, Register.RISKS.size());
        assertEquals(7, Register.CONTROLS.size());
        List<String> ids = Register.RISKS.stream().map(Risk::id).toList();
        for (Control c : Register.CONTROLS) {
            assertTrue(ids.containsAll(c.effects().keySet()), c.id());
        }
    }
}
