package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoreTest {

    private static Corpus small() {
        List<Ev> events = List.of(
                new Ev(1, Corpus.START, Map.of()), new Ev(2, Corpus.START + 1000, Map.of()), new Ev(3, Corpus.START + Corpus.DAY, Map.of()),
                new Ev(4, Corpus.START + Corpus.DAY + 1000, Map.of()), new Ev(5, Corpus.START + 2000, Map.of()), new Ev(6, Corpus.START + 3000, Map.of()));
        Map<Integer, Truth> truth = new LinkedHashMap<>();
        truth.put(1, new Truth(1, "attack", "r", "T1", 1));
        truth.put(2, new Truth(2, "attack", "r", "T1", 1));
        truth.put(3, new Truth(3, "evasion", "r", "T1", 2));
        truth.put(4, new Truth(4, "benign", "-", "-", 0));
        truth.put(5, new Truth(5, "lookalike", "r", "-", 0));
        truth.put(6, new Truth(6, "attack", "other", "T2", 3));
        return new Corpus(events, truth);
    }

    @Test
    void anAttackIsCaughtWhenAnyOfItsEventsAlerts() {
        Score s = Score.of("r", List.of(2), small());
        assertEquals(2, s.attacks());
        assertEquals(1, s.detected());
        assertEquals(1, s.missed());
    }

    @Test
    void anAlertOnABenignEventIsAFalsePositiveAndOnAnotherRulesAttackIsNeither() {
        Score s = Score.of("r", List.of(1, 2, 4, 5, 6), small());
        assertEquals(2, s.tpAlerts());
        assertEquals(2, s.falsePositives());
        assertEquals(1, s.otherAlerts());
        assertEquals(0.5, s.precision());
        assertEquals(0.5, s.recall());
    }

    @Test
    void alertsAreCountedPerSimulatedDayAndAgainstTheBudget() {
        Score s = Score.of("r", List.of(4, 5), small());
        assertEquals(1, s.falsePerDay()[0]);
        assertEquals(1, s.falsePerDay()[1]);
        assertEquals(1, s.worstDay());
        assertEquals(0, s.daysOver(1));
        assertEquals(2, s.daysOver(0));
    }

    @Test
    void noAlertsMeansPrecisionOneAndRecallZero() {
        Score s = Score.of("r", List.of(), small());
        assertEquals(1.0, s.precision());
        assertEquals(0.0, s.recall());
    }
}
