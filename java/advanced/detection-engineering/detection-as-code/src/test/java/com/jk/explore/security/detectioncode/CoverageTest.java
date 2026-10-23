package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoverageTest {

    private static Rule rule(String name, String tag) {
        return new Rule(Sigma.documents("name: " + name + "\ntags: [attack." + tag + "]\ndetection:\n  s:\n    X: a\n  condition: s\n").getFirst());
    }

    @Test
    void aTechniqueIsCoveredPartialUntestedOrAGap() {
        Map<String, Rule> rules = new LinkedHashMap<>();
        rules.put("r1", rule("r1", "t1001"));
        rules.put("r2", rule("r2", "t1002"));
        rules.put("r3", rule("r3", "t1004"));
        List<Ev> events = List.of(new Ev(1, Corpus.START, Map.of()), new Ev(2, Corpus.START, Map.of()), new Ev(3, Corpus.START, Map.of()), new Ev(4, Corpus.START, Map.of()));
        Map<Integer, Truth> truth = new LinkedHashMap<>();
        truth.put(1, new Truth(1, "attack", "r1", "T1001", 1));
        truth.put(2, new Truth(2, "attack", "r2", "T1002", 2));
        truth.put(3, new Truth(3, "evasion", "r2", "T1002", 3));
        truth.put(4, new Truth(4, "uncovered", "-", "T1003", 4));
        List<Coverage.Row> rows = Coverage.matrix(rules, new Corpus(events, truth), Set.of(1, 2));
        assertEquals(List.of("covered", "partial", "GAP no rule", "untested, no labelled attack"), rows.stream().map(Coverage.Row::status).toList());
    }
}
