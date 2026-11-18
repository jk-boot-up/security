package com.jk.explore.security.riskmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class YearStepsTest {

    private final Simulation sim = new Simulation(Register.RISKS, 3_000, 20260101L);

    @Test
    void theYearRedoneByHandIsExactlyTheSimulatedYearForEveryRisk() {
        for (int r = 0; r < Register.RISKS.size(); r++) {
            double[] years = sim.annualLosses(r, List.of());
            for (int t = 0; t < years.length; t++) {
                assertEquals(years[t], YearSteps.of(sim, r, t).total(), 0, Register.RISKS.get(r).id() + " year " + t);
            }
        }
    }

    @Test
    void theEventCountIsWhereTheDrawFallsBetweenTwoCumulativeChances() {
        for (int t = 0; t < 500; t++) {
            YearSteps y = YearSteps.of(sim, 4, t);
            int k = y.events();
            assertTrue(k == 0 || y.eventsDraw() > y.atMost().get(k - 1));
            assertTrue(y.eventsDraw() <= y.atMost().get(k));
            assertEquals(k, Simulation.poisson(y.rate(), y.eventsDraw()));
        }
    }

    @Test
    void theRateIsTheFrequencyRangeReadAtTheFirstDraw() {
        YearSteps y = YearSteps.of(sim, 0, 10);
        Tri f = Register.RISKS.get(0).frequency();
        assertEquals(f.at(y.frequencyDraw()), y.rate(), 0);
        assertEquals((f.mode() - f.min()) / (f.max() - f.min()), y.split(), 1e-12);
    }
}
