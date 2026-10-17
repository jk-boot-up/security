package com.jk.explore.security.threats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class RiskTest {

    @Test
    void theScoreIsLikelihoodTimesImpact() {
        Risk r = new Risk("R", "t", "v", "a", 4, 4);
        assertEquals(16, r.score());
        assertEquals(Rating.HIGH, r.rating());
    }

    @Test
    void ratingBandsChangeAtFiveAndTwelve() {
        assertEquals(Rating.LOW, Rating.of(5));
        assertEquals(Rating.MEDIUM, Rating.of(6));
        assertEquals(Rating.MEDIUM, Rating.of(12));
        assertEquals(Rating.HIGH, Rating.of(13));
    }

    @Test
    void scoresOutsideZeroToFiveAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Risk("R", "t", "v", "a", 6, 1));
        assertThrows(IllegalArgumentException.class, () -> new Risk("R", "t", "v", "a", 1, -1));
    }

    @Test
    void theStoreRisksRankInTheDemoOrder() {
        List<String> ids = ThreatsDemo.storeRisks().ranked().stream().map(Risk::id).toList();
        assertEquals(List.of("R1", "R4", "R3", "R2", "R6", "R5", "R7"), ids);
        assertEquals(73, ThreatsDemo.storeRisks().totalScore());
    }

    @Test
    void aTieGoesToTheHigherImpactThenTheLowerId() {
        RiskRegister register = new RiskRegister(List.of(
                new Risk("B", "t", "v", "a", 5, 2),
                new Risk("A", "t", "v", "a", 2, 5),
                new Risk("C", "t", "v", "a", 5, 2)));
        assertEquals(List.of("A", "B", "C"), register.ranked().stream().map(Risk::id).toList());
    }
}
