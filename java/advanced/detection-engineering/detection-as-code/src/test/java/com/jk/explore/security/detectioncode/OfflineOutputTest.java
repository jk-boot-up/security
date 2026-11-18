package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Acts ONE to FIVE need no tools: they must print exactly the golden output up to act SIX. */
class OfflineOutputTest {

    @Test
    void actsOneToFiveMatchTheGoldenFile() throws Exception {
        String golden = Files.readString(Path.of("docs", "expected-output.txt"));
        String offline = DetectionAsCodeDemo.offline(Sigma.load(DetectionAsCodeDemo.RULES), Corpus.generate(DetectionAsCodeDemo.SEED));
        Files.createDirectories(Path.of("build"));
        Files.writeString(Path.of("build", "offline-output.txt"), offline);
        int six = golden.indexOf("SIX. ");
        assertTrue(six > 0, "the golden output has an act SIX");
        assertEquals(golden.substring(0, six), offline);
    }

    @Test
    void theConditionWorkedOutFromItsSelectionsIsTheRulesOwnResult() throws Exception {
        Rule shell = Sigma.load(DetectionAsCodeDemo.RULES).get("web-server-shell");
        for (RuleTests.Case c : RuleTests.load(DetectionAsCodeDemo.RULES.resolve("tests")).get("web-server-shell")) {
            Map<String, String> event = c.events().getFirst().f();
            Map<String, Boolean> s = ConditionSteps.selections(shell, event);
            assertEquals(shell.matches(event), s.get("selection_parent") && s.get("selection_child") && !s.get("filter_healthcheck"), c.name());
        }
        for (Map<String, String> event : DetectionAsCodeDemo.THREE_EVENTS) {
            Map<String, Boolean> s = ConditionSteps.selections(shell, event);
            assertEquals(shell.matches(event), s.get("selection_parent") && s.get("selection_child") && !s.get("filter_healthcheck"));
        }
    }

    @Test
    void aLongQueryIsCutAtUnescapedSpacesAndBeforeEveryAnd() {
        assertEquals(java.util.List.of("(a:x\\ y OR b:z)", "AND (NOT c:w)"), DetectionAsCodeDemo.wrap("(a:x\\ y OR b:z) AND (NOT c:w)", 40));
    }
}
