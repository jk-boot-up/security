package com.jk.explore.security.logging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The steps the demo prints agree with what the real classes decide. */
class TeachingStepsTest {

    @Test
    void theLuhnStepsAddUpToTheCheckTheMaskerUses() {
        assertArrayEquals(new int[] {8, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1}, Masker.luhnSteps("4111111111111111"));
        assertEquals(30, IntStream.of(Masker.luhnSteps("4111111111111111")).sum());
        assertEquals(31, IntStream.of(Masker.luhnSteps("4111111111111112")).sum());
        assertArrayEquals(new int[] {1, 9}, Masker.luhnSteps("59"), "the last digit stays; the 5 is doubled to 10, and 10 - 9 = 1");
        assertTrue(Masker.luhn("5500000000000004"));
    }

    @Test
    void wrapKeepsEveryCharacterRespectsTheWidthAndKeepsACardNumberTogether() {
        String text = "checkout started: {name=Ana, card=4111 1111 1111 1111, cvv=737} " + LoggingDemo.databaseDown().getMessage();
        List<String> lines = LoggingDemo.wrap(text, 58);
        assertTrue(lines.stream().allMatch(l -> l.length() <= 58));
        assertEquals(text.replace(" ", ""), String.join("", lines).replace(" ", ""));
        assertTrue(lines.stream().anyMatch(l -> l.contains(LoggingDemo.CARD)));
    }

    @Test
    void theCanariesAreTheValuesTheCarelessLogKeeps() {
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            LoggingDemo.careless(log.logger());
            LoggingDemo.canaries().values().forEach(v -> assertTrue(log.output().contains(v), v));
        }
    }
}
