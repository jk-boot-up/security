package com.jk.explore.security.attacktree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The worked-out steps of every gate give exactly what Analysis computes. */
class GateStepsTest {

    private static int check(Node n) {
        if (!(n instanceof Node.Gate g)) {
            return 0;
        }
        GateSteps steps = GateSteps.of(g);
        Analysis a = Analysis.of(g);
        assertEquals(a.minCost(), steps.minCost(), 1e-9, g.name());
        assertEquals(a.probability(), steps.probability(), 1e-9, g.name());
        return 1 + g.children().stream().mapToInt(GateStepsTest::check).sum();
    }

    @Test
    void everyGateOfTheTreeAgreesWithAnalysis() {
        assertEquals(9, check(BankTree.baseline()));
        assertEquals(9, check(Defence.all().getFirst().applyTo(BankTree.baseline())));
    }

    @Test
    void theStepsReadAsArithmetic() {
        GateSteps phish = GateSteps.of(AttackTreeDemo.gate(BankTree.baseline(), "Phish her password and one-time code"));
        assertEquals("300 + 50 = 350", phish.cost());
        assertEquals("0.9 x 0.2 = 18%", phish.chance());
        GateSteps login = GateSteps.of(AttackTreeDemo.gate(BankTree.baseline(), "Log in as Ana"));
        assertEquals("min(350, 100, 1,600, 2,000) = 100", login.cost());
        assertEquals("1 - (1-0.18)(1-0.12)(1-0.045)(1-0.05) = 35%", login.chance());
    }
}
