package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The rules, their tests and their budgets as they are in the repository. */
class RulesAsCodeTest {

    private static Map<String, Rule> rules() throws Exception {
        return Sigma.load(DetectionAsCodeDemo.RULES);
    }

    @Test
    void everyRuleHasATitleAnIdALevelAndAnAttackTag() throws Exception {
        for (Rule r : rules().values()) {
            assertTrue(r.doc().containsKey("title") && r.doc().containsKey("level") && r.doc().containsKey("status"), r.name());
            assertTrue(String.valueOf(r.doc().get("id")).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), r.name());
            assertFalse(r.techniques().isEmpty(), r.name());
        }
    }

    @Test
    void everyRuleHasATestFileWithCasesBothWays() throws Exception {
        Map<String, List<RuleTests.Case>> tests = RuleTests.load(DetectionAsCodeDemo.RULES.resolve("tests"));
        for (String name : rules().keySet()) {
            assertTrue(tests.containsKey(name), name + " has no test file");
            assertTrue(tests.get(name).stream().anyMatch(RuleTests.Case::expect), name + " has no should-match case");
            assertTrue(tests.get(name).stream().anyMatch(c -> !c.expect()), name + " has no should-not-match case");
        }
    }

    @Test
    void everyRuleThatAlertsHasABudget() throws Exception {
        assertEquals(DetectionAsCodeDemo.scored(rules()).stream().sorted().toList(), DetectionAsCodeDemo.budgets().keySet().stream().sorted().toList());
    }

    @Test
    void everyCaseOfEveryRuleTestPasses() throws Exception {
        Map<String, Rule> rules = rules();
        for (Map.Entry<String, List<RuleTests.Case>> e : RuleTests.load(DetectionAsCodeDemo.RULES.resolve("tests")).entrySet()) {
            assertEquals(List.of(), RuleTests.failures(e.getValue(), rules.get(e.getKey()), rules), e.getKey());
        }
    }

    @Test
    void everySmallChangeToARuleIsCaughtByItsTests() throws Exception {
        Map<String, Rule> rules = rules();
        Map<String, List<RuleTests.Case>> tests = RuleTests.load(DetectionAsCodeDemo.RULES.resolve("tests"));
        List<String> survivors = new ArrayList<>();
        for (Rule r : rules.values()) {
            for (Mutator.Mutant m : Mutator.of(r)) {
                if (RuleTests.failures(tests.get(r.name()), m.rule(), Mutator.with(rules, m.rule())).isEmpty()) {
                    survivors.add(m.label());
                }
            }
        }
        assertEquals(List.of(), survivors);
    }

    @Test
    void withTheShouldNotMatchCasesRemovedTheLoosenedRulesSurvive() throws Exception {
        Map<String, Rule> rules = rules();
        List<RuleTests.Case> onlyPositive = RuleTests.load(DetectionAsCodeDemo.RULES.resolve("tests")).get("web-server-shell").stream().filter(RuleTests.Case::expect).toList();
        long survived = Mutator.of(rules.get("web-server-shell")).stream().filter(m -> m.direction().equals("loosen"))
                .filter(m -> RuleTests.failures(onlyPositive, m.rule(), Mutator.with(rules, m.rule())).isEmpty()).count();
        assertTrue(survived >= 2, "loosening should go unnoticed without negative cases, survived " + survived);
    }

    @Test
    void theHealthCheckFilterIsWhatKeepsTheFalseAlarmsOut() throws Exception {
        Map<String, Rule> rules = rules();
        Corpus c = Corpus.generate(DetectionAsCodeDemo.SEED);
        Mutator.Mutant loose = Mutator.of(rules.get("web-server-shell")).stream().filter(m -> m.what().equals("drop filter_healthcheck")).findFirst().orElseThrow();
        assertTrue(Score.of("web-server-shell", Mutator.with(rules, loose.rule()), c).falsePositives() > 10 * Score.of("web-server-shell", rules, c).falsePositives());
    }

    @Test
    void noRuleFileHoldsAKeyOrTokenShapedString() throws Exception {
        try (var files = Files.walk(Path.of("rules"))) {
            String all = files.filter(Files::isRegularFile).map(p -> {
                try {
                    return Files.readString(p);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.joining("\n"));
            assertFalse(all.matches("(?s).*(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{20}|sk_live_|BEGIN [A-Z ]*PRIVATE KEY).*"));
        }
    }

    @Test
    void theAttackCorpusIsCaughtByTheRulesItWasLabelledFor() throws Exception {
        Map<String, Rule> rules = rules();
        Corpus c = Corpus.generate(DetectionAsCodeDemo.SEED);
        for (String name : DetectionAsCodeDemo.scored(rules)) {
            Score s = Score.of(name, rules, c);
            assertTrue(s.detected() >= 8 && s.missed() >= 2, name + " " + s);
        }
    }
}
