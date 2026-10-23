package com.jk.explore.security.secretscan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void aFindingThatContainsARealSecretCountsAsFound() {
        Score s = Score.of(List.of(new Finding("r", "f", Planted.AWS.value())));
        assertEquals(List.of(Planted.AWS), s.found());
        assertEquals(4, s.missed().size());
    }

    @Test
    void aFindingOnALookAlikeIsAFalseAlarm() {
        Score s = Score.of(List.of(new Finding("r", "f", Planted.AWS_DOC.value())));
        assertEquals(List.of(Planted.AWS_DOC), s.falseAlarms());
    }

    @Test
    void aFindingOnNothingPlantedIsAnotherAlarm() {
        assertEquals(1, Score.of(List.of(new Finding("r", "f", "something else"))).unrelated());
    }

    @Test
    void base64OfARealSecretStillCountsAsFound() {
        String encoded = java.util.Base64.getEncoder().encodeToString(Planted.GITHUB.value().getBytes());
        assertEquals(List.of(Planted.GITHUB), Score.of(List.of(new Finding("r", "f", encoded))).found());
    }

    @Test
    void theLineReadsLikeTheDemo() {
        assertEquals("0 of 5 real secrets found, 0 look-alikes flagged, 0 other alarms", Score.of(List.of()).line());
    }
}
