package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CorpusTest {

    private static final Corpus CORPUS = Corpus.generate(DetectionAsCodeDemo.SEED);

    @Test
    void theSameSeedMakesTheSameLogs() {
        List<String> a = CORPUS.events().stream().map(Ev::json).toList();
        List<String> b = Corpus.generate(DetectionAsCodeDemo.SEED).events().stream().map(Ev::json).toList();
        assertEquals(a, b);
    }

    @Test
    void anotherSeedMakesOtherLogs() {
        assertNotEquals(CORPUS.events().stream().map(Ev::json).toList(), Corpus.generate(DetectionAsCodeDemo.SEED + 1).events().stream().map(Ev::json).toList());
    }

    @Test
    void everyEventHasATruthAndEventsAreInTimeOrder() {
        assertEquals(CORPUS.events().size(), CORPUS.truth().size());
        for (int i = 1; i < CORPUS.events().size(); i++) {
            assertTrue(CORPUS.events().get(i - 1).ms() <= CORPUS.events().get(i).ms());
            assertEquals(i, CORPUS.events().get(i - 1).id());
        }
    }

    @Test
    void theWeekIsSevenDaysAndHasThousandsOfBenignEvents() {
        assertTrue(CORPUS.events().stream().allMatch(e -> Corpus.day(e) >= 0 && Corpus.day(e) < Corpus.DAYS));
        assertTrue(CORPUS.truth().values().stream().filter(t -> t.kind().equals("benign")).count() > 20_000);
    }

    @Test
    void everyRuleHasAttacksAndEvasionsAndLookalikes() {
        for (String rule : List.of("web-server-shell", "new-admin-user", "credential-dump-tools", "failed-login-burst", "download-pipe-shell", "log-clearing", "scheduled-task-persistence", "web-injection-probe")) {
            Set<String> kinds = CORPUS.truth().values().stream().filter(t -> t.rule().equals(rule)).map(Truth::kind).collect(Collectors.toSet());
            Set<String> wanted = rule.equals("failed-login-burst") ? Set.of("attack", "evasion", "lookalike") : Set.of("attack", "evasion", "lookalike", "filtered");
            assertTrue(kinds.containsAll(wanted), rule + " has " + kinds);
        }
    }

    @Test
    void theJsonHasTheSigmaFieldNamesAndNoTruth() {
        String json = CORPUS.events().getFirst().json();
        assertTrue(json.startsWith("{\"id\":1,\"timestamp\":\"2026-03-02T"));
        assertTrue(CORPUS.events().stream().noneMatch(e -> e.json().contains("evasion") && e.f().containsKey("kind")));
    }
}
