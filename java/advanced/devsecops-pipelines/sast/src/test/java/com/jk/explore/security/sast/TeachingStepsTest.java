package com.jk.explore.security.sast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The worked-out steps the demo prints, checked with no tool. */
class TeachingStepsTest {

    @Test
    void taintTravelsFromTheSourceThroughAVariableToTheSink() {
        List<TaintSteps.Step> steps = TaintSteps.follow(List.of(
                "String name = request.getParameter(\"name\");",
                "String sql = \"SELECT * FROM t WHERE name = '\" + name + \"'\";",
                "return stmt.executeQuery(sql).getString(1);"), 22, "getParameter", "executeQuery");
        assertEquals(List.of(
                new TaintSteps.Step(22, "name", "tainted: from the source"),
                new TaintSteps.Step(23, "sql", "tainted: from name"),
                new TaintSteps.Step(24, "executeQuery(sql)", "a tainted value reaches the sink")), steps);
    }

    @Test
    void aConstantQueryGivesNoFinding() {
        List<TaintSteps.Step> steps = TaintSteps.follow(List.of("return stmt.executeQuery(\"SELECT 1\");"), 1, "getParameter", "executeQuery");
        assertTrue(steps.isEmpty());
    }

    @Test
    void theRuleFileNamesTheSourceTheSinkAndTheSanitizers() throws Exception {
        RuleFile rules = RuleFile.read(Path.of(SastDemo.TAINT));
        assertEquals(List.of("$REQ.getParameter(...)"), rules.patterns("sql-taint", "pattern-sources"));
        assertEquals(List.of("$S.executeQuery(...)"), rules.patterns("sql-taint", "pattern-sinks"));
        assertEquals(List.of(SastDemo.SANITIZER, "Validate.fileName(...)"), rules.patterns("sql-taint", "pattern-sanitizers"));
        assertEquals(List.of("new File($DIR, $NAME)"), rules.patterns("path-taint", "pattern-sinks"));
    }

    @Test
    void theTestRunIsReadFromSemgrepJson() {
        Semgrep.TestRun run = Semgrep.parseTest("""
                {"config_missing_tests": ["/x/rules/pattern.yml"], "results": {"/x/taint.yml": {"checks": {
                  "sql-taint": {"passed": false, "matches": {"/x/taint.java": {"expected_lines": [10, 16], "reported_lines": [10, 16, 22]}}},
                  "path-taint": {"passed": true, "matches": {"/x/taint.java": {"expected_lines": [42], "reported_lines": [42]}}}}}}}
                """);
        assertEquals(List.of("path-taint", "sql-taint"), run.checks().stream().map(Semgrep.Check::rule).toList());
        assertEquals(1, run.passed());
        assertEquals(List.of(10, 16, 22), run.checks().get(1).reported());
        assertFalse(run.checks().get(1).passed());
        assertEquals(List.of("pattern.yml"), run.missingTests());
    }

    @Test
    void recallAndPrecisionAreWorkedOut() {
        List<Marker> markers = List.of(new Marker("A.java", 1, true, "sql-a"), new Marker("A.java", 2, true, "sql-b"),
                new Marker("A.java", 3, false, "sql-c"));
        Score s = Score.of(markers, List.of(new Finding("r", "A.java", 1), new Finding("r", "A.java", 3)), "sql-");
        assertEquals("1/2 = 50%", s.recall());
        assertEquals("1/2 = 50%", s.precision());
    }
}
