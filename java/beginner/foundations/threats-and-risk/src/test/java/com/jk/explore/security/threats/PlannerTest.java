package com.jk.explore.security.threats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlannerTest {

    private final Planner planner = new Planner();

    @Test
    void theBestValueIsBoughtFirstWhileTheBudgetLasts() {
        Plan plan = planner.plan(ThreatsDemo.storeRisks(), ThreatsDemo.storeTreatments(), ThreatsDemo.BUDGET);
        assertEquals(List.of("R4", "R1", "R3", "R2"), plan.funded().stream().map(Treatment::riskId).toList());
        assertEquals(List.of("R6"), plan.unfunded().stream().map(Treatment::riskId).toList());
        assertEquals(9, plan.spent());
    }

    @Test
    void totalRiskFallsFromSeventyThreeToThirtyFive() {
        Plan plan = planner.plan(ThreatsDemo.storeRisks(), ThreatsDemo.storeTreatments(), ThreatsDemo.BUDGET);
        assertEquals(73, ThreatsDemo.storeRisks().totalScore());
        assertEquals(35, plan.after().totalScore());
        Risk worst = plan.after().ranked().get(0);
        assertEquals("R6", worst.id());
        assertEquals(10, worst.score());
    }

    @Test
    void aZeroBudgetFundsNothingAndChangesNothing() {
        Plan plan = planner.plan(ThreatsDemo.storeRisks(), ThreatsDemo.storeTreatments(), 0);
        assertTrue(plan.funded().isEmpty());
        assertEquals(73, plan.after().totalScore());
    }

    @Test
    void aTreatmentThatRemovesNothingIsNeverFunded() {
        Plan plan = planner.plan(ThreatsDemo.storeRisks(), ThreatsDemo.storeTreatments(), 100);
        assertTrue(plan.funded().stream().noneMatch(t -> t.strategy() == Strategy.ACCEPT));
    }

    @Test
    void avoidingARiskRemovesItsWholeScore() {
        Risk r4 = ThreatsDemo.storeRisks().byId("R4");
        Treatment avoid = ThreatsDemo.storeTreatments().stream().filter(t -> t.riskId().equals("R4")).findFirst().orElseThrow();
        assertEquals(r4.score(), Planner.reduction(ThreatsDemo.storeRisks(), avoid));
    }
}
