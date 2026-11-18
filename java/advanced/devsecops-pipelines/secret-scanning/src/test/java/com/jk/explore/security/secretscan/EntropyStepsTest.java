package com.jk.explore.security.secretscan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntropyStepsTest {

    private static final String ENCODED = Base64.getEncoder().encodeToString(Planted.BASE64.value().getBytes());

    @Test
    void theStepsAddUpToTheDetectorsEntropy() {
        for (Planted p : Planted.ALL) {
            assertEquals(Detector.entropy(p.value()), EntropySteps.total(p.value()), 1e-9);
        }
        assertEquals(Detector.entropy(ENCODED), EntropySteps.total(ENCODED), 1e-9);
    }

    @Test
    void theBase64KeyHasFourGroupsOfCharacters() {
        List<EntropySteps.Group> g = EntropySteps.groups(ENCODED);
        assertEquals(List.of(21, 6, 2, 1), g.stream().map(EntropySteps.Group::characters).toList());
        assertEquals(List.of(1, 2, 3, 5), g.stream().map(EntropySteps.Group::times).toList());
        assertEquals("6 characters twice:     6 x 2/44 x log2(44/2) = 1.216", g.get(1).line());
    }

    @Test
    void hexCanNeverReachTheThreshold() {
        assertEquals(16, EntropySteps.distinct("0123456789abcdef"));
        assertEquals(4.0, EntropySteps.total("0123456789abcdef"), 1e-9);
        assertEquals(true, 4.0 < Detector.THRESHOLD);
    }
}
