package com.jk.explore.security.riskmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTest {

    @Test
    void aTriangleStaysBetweenItsEndsAndHitsTheModeAtTheSplit() {
        Tri t = new Tri(10, 20, 50);
        assertEquals(10, t.at(0), 1e-9);
        assertEquals(50, t.at(0.999999999), 0.01);
        assertEquals(20, t.at((20.0 - 10) / (50 - 10)), 1e-9);
    }

    @Test
    void theMeanOfATriangleIsTheAverageOfThePoints() {
        assertEquals(26.6667, new Tri(10, 20, 50).mean(), 1e-3);
    }

    @Test
    void aTriangleNeedsItsPointsInOrder() {
        assertThrows(IllegalArgumentException.class, () -> new Tri(5, 1, 9));
    }

    @Test
    void poissonWalksUpTheDistributionFunction() {
        assertEquals(0, Simulation.poisson(2, 0.01));
        assertEquals(0, Simulation.poisson(2, Math.exp(-2) - 1e-9));
        assertEquals(1, Simulation.poisson(2, Math.exp(-2) + 1e-9));
        assertTrue(Simulation.poisson(2, 0.99999) >= 6);
    }

    @Test
    void theSimulatedMeanAgreesWithMeanFrequencyTimesMeanLoss() {
        Risk r = Register.RISKS.get(4);
        Simulation sim = new Simulation(List.of(r), 100_000, 1);
        double simulated = Simulation.mean(sim.annualLosses(0, List.of()));
        assertEquals(r.analyticAnnualLoss(), simulated, r.analyticAnnualLoss() * 0.03);
    }

    @Test
    void theSameSeedGivesTheSameYears() {
        Simulation a = new Simulation(Register.RISKS, 2_000, 5);
        Simulation b = new Simulation(Register.RISKS, 2_000, 5);
        assertEquals(Simulation.mean(a.portfolio(List.of())), Simulation.mean(b.portfolio(List.of())), 0);
    }

    @Test
    void aControlThatKeepsAThirdOfTheLossCutsTheMeanByAboutTwoThirds() {
        Simulation sim = new Simulation(Register.RISKS, 20_000, 3);
        double before = Simulation.mean(sim.annualLosses(0, List.of()));
        double after = Simulation.mean(sim.annualLosses(0, List.of(Register.CONTROLS.get(0))));
        assertEquals(0.3, after / before, 1e-9);
    }

    @Test
    void aControlThatDoesNotTouchARiskLeavesItAlone() {
        Simulation sim = new Simulation(Register.RISKS, 5_000, 3);
        assertEquals(Simulation.mean(sim.annualLosses(7, List.of())), Simulation.mean(sim.annualLosses(7, List.of(Register.CONTROLS.get(0)))), 0);
    }

    @Test
    void percentilesAndExceedanceAgree() {
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertEquals(10, Simulation.percentile(x, 0.99));
        assertEquals(0.3, Simulation.exceed(x, 7), 1e-9);
    }
}
