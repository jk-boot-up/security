package com.jk.explore.security.sast;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SastDemo {

    static final Path CODE = Path.of("corpus");
    static final String PATTERN = "rules/pattern.yml";
    static final String TAINT = "rules/taint.yml";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Semgrep semgrep = new Semgrep(new Venv(Path.of("build")));
        List<Marker> markers = Marker.in(CODE);
        long vulnerable = markers.stream().filter(Marker::vulnerable).count();

        out.append("--- the code under test\n");
        out.append("files: " + Files.list(CODE.resolve("bank")).count() + ", flaws planted: " + vulnerable + ", safe look-alikes: " + (markers.size() - vulnerable) + "\n");

        out.append("--- a pattern rule and a taint rule on the SQL\n");
        Semgrep.Scan pattern = semgrep.scan(CODE, false, PATTERN);
        Semgrep.Scan taint = semgrep.scan(CODE, false, TAINT);
        Score p = Score.of(markers, pattern.findings(), "sql-");
        Score t = Score.of(markers, taint.findings(), "sql-");
        out.append("pattern rule: " + p.line() + "\n");
        out.append("taint rule:   " + t.line() + "\n");
        out.append("pattern rule flags: " + Score.names(p.flaggedSafe()) + "\n");
        out.append("taint rule misses: " + Score.names(t.missed()) + ", flags: " + Score.names(t.flaggedSafe()) + "\n");

        out.append("--- every flaw class with the taint rules\n");
        Score all = Score.of(markers, taint.findings(), "");
        out.append("taint rules and weak hash: " + all.line() + "\n");
        for (String kind : List.of("command", "path", "weak-hash")) {
            Score s = Score.of(markers, taint.findings(), kind);
            out.append("  " + kind + ": " + s.line() + "\n");
        }

        out.append("--- tests for the rules\n");
        int[] tests = semgrep.test(Path.of("rules"));
        out.append("semgrep --test: " + tests[0] + " of " + tests[1] + " checks pass, rule files with no test: " + tests[2] + "\n");
        Path mutant = Files.createTempDirectory("mutant");
        Files.writeString(mutant.resolve("taint.yml"), Files.readString(Path.of(TAINT)).replace("      - pattern: Integer.parseInt(...)\n", ""));
        Files.copy(Path.of("rules/taint.java"), mutant.resolve("taint.java"));
        int[] broken = semgrep.test(mutant);
        out.append("the same tests on a rule with its parseInt sanitizer removed: " + broken[0] + " of " + broken[1] + " checks pass\n");

        out.append("--- triage\n");
        Path copy = Semgrep.copy(CODE);
        Path controller = copy.resolve("bank/AccountController.java");
        List<String> lines = Files.readAllLines(controller);
        int sink = Marker.in(copy).stream().filter(m -> m.id().equals("sql-allowlisted")).findFirst().orElseThrow().line();
        Semgrep.Scan before = semgrep.scan(copy, true, TAINT);
        out.append("findings: " + before.findings().size() + " (" + all.found().size() + " real, " + all.flaggedSafe().size() + " false), exit code " + before.exit() + "\n");
        Files.write(controller, Suppressions.insertBefore(lines, sink, "        // nosemgrep: rules.sql-taint -- the column is checked against an allowlist above (owner: payments-team)"));
        Semgrep.Scan after = semgrep.scan(copy, true, TAINT);
        Suppressions.Count good = Suppressions.count(copy);
        out.append("with a suppression that says why and who: " + after.findings().size() + " findings, suppressions " + good.total() + ", well written " + good.valid() + "\n");
        Files.write(controller, Suppressions.insertBefore(Files.readAllLines(controller), 20, "        // nosemgrep: rules.sql-taint"));
        Suppressions.Count bad = Suppressions.count(copy);
        out.append("a second one with no reason: suppressions " + bad.total() + ", well written " + bad.valid() + ", the build check fails: " + yes(bad.total() != bad.valid()) + "\n");
        Semgrep.Scan clean = semgrep.scan(CODE.resolve("bank/Validate.java"), true, TAINT);
        out.append("a file with no flaw: " + clean.findings().size() + " findings, exit code " + clean.exit() + "\n");

        out.append("--- the bill\n");
        out.append("a taint rule follows input inside a method: the sink in another file is missed\n");
        out.append("a rule that sees no check cannot know that an allowlist is one: write the suppression, with a reason\n");
        out.append("a rule without a test is a guess, and a test is only as good as its cases\n");
        return out.toString();
    }
}
