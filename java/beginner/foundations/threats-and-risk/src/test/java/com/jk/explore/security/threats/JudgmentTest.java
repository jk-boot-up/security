package com.jk.explore.security.threats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudgmentTest {

    private final RiskRegister base = ThreatsDemo.storeRisks();

    @Test
    void twoReviewersWithTheSameRulesRankDifferently() {
        RiskRegister engineering = base.rescored(Map.of("R3", new int[] {2, 3}));
        RiskRegister finance = base.rescored(Map.of("R3", new int[] {5, 4}));
        assertEquals("R1", engineering.ranked().get(0).id());
        assertEquals("R3", finance.ranked().get(0).id());
        assertNotEquals(engineering.ranked().get(0).id(), finance.ranked().get(0).id());
    }

    @Test
    void rescoringLeavesTheOriginalRegisterAlone() {
        base.rescored(Map.of("R3", new int[] {5, 5}));
        assertEquals(12, base.byId("R3").score());
    }

    @Test
    void topThreeUnderEachViewMatchTheDemo() {
        RiskRegister engineering = base.rescored(Map.of("R3", new int[] {2, 3}, "R7", new int[] {4, 1}));
        RiskRegister finance = base.rescored(Map.of("R3", new int[] {5, 4}, "R7", new int[] {4, 3}));
        assertEquals(List.of("R1", "R4", "R2"), engineering.ranked().stream().limit(3).map(Risk::id).toList());
        assertEquals(List.of("R3", "R1", "R4"), finance.ranked().stream().limit(3).map(Risk::id).toList());
    }
}
