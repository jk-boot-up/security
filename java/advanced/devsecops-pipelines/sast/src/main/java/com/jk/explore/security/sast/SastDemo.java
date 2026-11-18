package com.jk.explore.security.sast;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SastDemo {

    static final Path CODE = Path.of("corpus");
    static final String PATTERN = "rules/pattern.yml";
    static final String TAINT = "rules/taint.yml";
    static final String SANITIZER = "Integer.parseInt(...)";
    static final String WHY = "the column is checked against an allowlist above";
    static final String OWNER = "payments-team";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static String pad(String s, int width) {
        return s + " ".repeat(Math.max(1, width - s.length()));
    }

    /** "found" or "missed" for a flaw, "flagged" or "quiet" for a safe line. */
    static String verdict(Marker m, List<Finding> findings) {
        boolean hit = findings.stream().anyMatch(f -> f.key().equals(m.key()));
        return m.vulnerable() ? (hit ? "found" : "missed") : (hit ? "flagged" : "quiet");
    }

    static String tag(Marker m) {
        return (m.vulnerable() ? "VULN " : "SAFE ") + m.id();
    }

    static String where(Finding f) {
        return f.file().replace(".java", "") + ":" + f.line();
    }

    static String lines(List<Integer> ns) {
        return String.join(" ", ns.stream().map(String::valueOf).toList());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Semgrep semgrep = new Semgrep(new Venv(Path.of("build")));
        List<Marker> markers = Marker.in(CODE);
        long vulnerable = markers.stream().filter(Marker::vulnerable).count();
        Semgrep.Scan pattern = semgrep.scan(CODE, false, PATTERN);
        Semgrep.Scan taint = semgrep.scan(CODE, false, TAINT);

        out.append("ONE. One flaw, as a taint rule sees it.\n");
        Marker variable = markers.stream().filter(m -> m.id().equals("sql-variable")).findFirst().orElseThrow();
        int first = variable.line() - 2;
        List<String> code = Files.readAllLines(CODE.resolve("bank").resolve(variable.file())).subList(first - 1, variable.line());
        out.append("  THE CODE (" + variable.file() + ", lines " + first + " to " + variable.line() + "):\n");
        for (String line : code) {
            out.append("    " + line.replaceAll("\\s*// (VULN|SAFE).*", "").strip() + "\n");
        }
        RuleFile rules = RuleFile.read(Path.of(TAINT));
        out.append("  THE RULE sql-taint (" + TAINT + "):\n");
        out.append("    source     " + String.join(", ", rules.patterns("sql-taint", "pattern-sources")) + "\n");
        out.append("    sink       " + String.join(", ", rules.patterns("sql-taint", "pattern-sinks")) + "\n");
        out.append("    sanitizer  " + String.join(", ", rules.patterns("sql-taint", "pattern-sanitizers")) + "\n");
        out.append("  HOW THE TAINT TRAVELS (worked out by hand):\n");
        for (TaintSteps.Step s : TaintSteps.follow(code, first, "getParameter", "executeQuery")) {
            out.append("    line " + s.line() + "  " + pad(s.what(), 19) + s.why() + "\n");
        }
        out.append("  WHAT SEMGREP REPORTS:\n");
        Optional<Finding> reported = taint.findings().stream().filter(f -> f.key().equals(variable.key())).findFirst();
        out.append("    " + reported.map(f -> f.rule() + "  " + where(f)).orElse("nothing") + ", the same line: " + yes(reported.isPresent()) + "\n");

        out.append("TWO. A pattern rule and a taint rule on the SQL.\n");
        out.append("  THE CODE UNDER TEST (corpus/bank):\n");
        out.append("    files " + Files.list(CODE.resolve("bank")).count() + ", flaws planted " + vulnerable + ", safe look-alikes " + (markers.size() - vulnerable) + "\n");
        out.append("    each such line ends in // VULN or // SAFE\n");
        out.append("  THE RULES:\n");
        out.append("    pattern  sql-concat: executeQuery($Q + ...), executeQuery($Q)\n");
        out.append("    taint    sql-taint: from getParameter to executeQuery\n");
        List<Marker> sql = markers.stream().filter(m -> m.id().startsWith("sql-")).toList();
        out.append("  THE " + sql.size() + " SQL LINES, AND WHAT EACH RULE SAYS:\n");
        out.append("    " + pad("the line's marker", 24) + pad("pattern", 9) + "taint\n");
        for (Marker m : sql) {
            out.append("    " + pad(tag(m), 24) + pad(verdict(m, pattern.findings()), 9) + verdict(m, taint.findings()) + "\n");
        }
        Score p = Score.of(markers, pattern.findings(), "sql-");
        Score t = Score.of(markers, taint.findings(), "sql-");
        out.append("  HOW THE SCORE IS MADE:\n");
        out.append("    " + pad("", 28) + pad("pattern", 12) + "taint\n");
        out.append("    " + pad("flaws found", 28) + pad(p.found().size() + " of " + p.vulnerable(), 12) + t.found().size() + " of " + t.vulnerable() + "\n");
        out.append("    " + pad("safe lines flagged", 28) + pad(p.flaggedSafe().size() + " of " + p.safeTotal(), 12) + t.flaggedSafe().size() + " of " + t.safeTotal() + "\n");
        out.append("    " + pad("recall = found / flaws", 28) + pad(p.recall(), 12) + t.recall() + "\n");
        out.append("    " + pad("precision = real / flagged", 28) + pad(p.precision(), 12) + t.precision() + "\n");

        out.append("THREE. Every flaw class with the taint rules.\n");
        out.append("  THE RULES (" + TAINT + "):\n");
        out.append("    sql-taint, command-taint, path-taint: taint\n");
        out.append("    weak-hash: a pattern, MD5 or SHA-1\n");
        out.append("  WHAT THEY REPORT (" + taint.findings().size() + " findings):\n");
        for (Finding f : taint.findings()) {
            String truth = markers.stream().filter(m -> m.key().equals(f.key())).map(SastDemo::tag).findFirst().orElse("no marker");
            out.append("    " + pad(f.rule(), 15) + pad(where(f), 22) + truth + "\n");
        }
        out.append("  BY CLASS:\n");
        out.append("    " + pad("class", 11) + pad("flaws found", 14) + "safe lines flagged\n");
        for (String kind : List.of("sql", "command", "path", "weak-hash", "")) {
            Score s = Score.of(markers, taint.findings(), kind);
            out.append("    " + pad(kind.isEmpty() ? "all" : kind, 11) + pad(s.found().size() + " of " + s.vulnerable(), 14) + s.flaggedSafe().size() + " of " + s.safeTotal() + "\n");
        }

        out.append("FOUR. Tests for the rules.\n");
        List<String> testFile = Files.readAllLines(Path.of("rules/taint.java"));
        out.append("  THE TEST FILE (rules/taint.java):\n");
        out.append("    // ruleid: <rule>  the next line must be flagged: " + testFile.stream().filter(l -> l.contains("// ruleid:")).count() + "\n");
        out.append("    // ok: <rule>      the next line must not be: " + testFile.stream().filter(l -> l.contains("// ok:")).count() + "\n");
        Semgrep.TestRun tests = semgrep.test(Path.of("rules"));
        out.append("  BEFORE THE CHANGE (semgrep --test rules):\n");
        for (Semgrep.Check c : tests.checks()) {
            out.append("    " + pad(c.rule(), 15) + pad(c.passed() ? "pass" : "FAIL", 6) + "expected " + lines(c.expected()) + ", reported " + lines(c.reported()) + "\n");
        }
        out.append("    " + tests.passed() + " of " + tests.checks().size() + " pass; rule files with no test: " + (tests.missingTests().isEmpty() ? "none" : String.join(", ", tests.missingTests())) + "\n");
        Path mutant = Files.createTempDirectory("mutant");
        Files.writeString(mutant.resolve("taint.yml"), Files.readString(Path.of(TAINT)).replace("      - pattern: " + SANITIZER + "\n", ""));
        Files.copy(Path.of("rules/taint.java"), mutant.resolve("taint.java"));
        out.append("  THE CHANGE:\n");
        out.append("    the sanitizer " + SANITIZER + " removed from sql-taint\n");
        Semgrep.TestRun broken = semgrep.test(mutant);
        out.append("  AFTER THE CHANGE:\n");
        List<Integer> extra = new ArrayList<>();
        for (Semgrep.Check c : broken.checks()) {
            if (!c.passed()) {
                out.append("    " + pad(c.rule(), 15) + pad("FAIL", 6) + "expected " + lines(c.expected()) + ", reported " + lines(c.reported()) + "\n");
                c.reported().stream().filter(n -> !c.expected().contains(n)).forEach(extra::add);
            }
        }
        out.append("    " + broken.passed() + " of " + broken.checks().size() + " pass\n");
        for (int n : extra) {
            out.append("  WHY LINE " + n + " (rules/taint.java, lines " + (n - 2) + " to " + n + "):\n");
            for (String line : testFile.subList(n - 3, n)) {
                out.append("    " + line.strip() + "\n");
            }
        }

        out.append("FIVE. Triage.\n");
        Score all = Score.of(markers, taint.findings(), "");
        Path copy = Semgrep.copy(CODE);
        Path controller = copy.resolve("bank/AccountController.java");
        List<String> source = Files.readAllLines(controller);
        int sink = Marker.in(copy).stream().filter(m -> m.id().equals("sql-allowlisted")).findFirst().orElseThrow().line();
        Semgrep.Scan before = semgrep.scan(copy, true, TAINT);
        out.append("  BEFORE THE SUPPRESSION (semgrep --error):\n");
        out.append("    findings " + before.findings().size() + " (" + all.found().size() + " real, " + all.flaggedSafe().size() + " false), exit code " + before.exit() + ": the build fails\n");
        Files.write(controller, Suppressions.insertBefore(source, sink, "        // nosemgrep: rules.sql-taint -- " + WHY + " (owner: " + OWNER + ")"));
        out.append("  THE SUPPRESSION (a comment above AccountController line " + sink + "):\n");
        out.append("    rule   nosemgrep: rules.sql-taint\n");
        out.append("    why    -- " + WHY + "\n");
        out.append("    owner  (owner: " + OWNER + ")\n");
        Semgrep.Scan after = semgrep.scan(copy, true, TAINT);
        Suppressions.Count good = Suppressions.count(copy);
        out.append("  AFTER THE SUPPRESSION:\n");
        out.append("    findings " + after.findings().size() + ", exit code " + after.exit() + ": the real flaws still fail the build\n");
        out.append("    suppressions " + good.total() + ", well written " + good.valid() + "\n");
        Files.write(controller, Suppressions.insertBefore(Files.readAllLines(controller), 20, "        // nosemgrep: rules.sql-taint"));
        Suppressions.Count bad = Suppressions.count(copy);
        out.append("  A SECOND SUPPRESSION, WITH NO REASON (above line 20):\n");
        out.append("    // nosemgrep: rules.sql-taint\n");
        out.append("    suppressions " + bad.total() + ", well written " + bad.valid() + ", the build check fails: " + yes(bad.total() != bad.valid()) + "\n");
        Semgrep.Scan clean = semgrep.scan(CODE.resolve("bank/Validate.java"), true, TAINT);
        out.append("  A FILE WITH NO FLAW (Validate.java):\n");
        out.append("    findings " + clean.findings().size() + ", exit code " + clean.exit() + ": the build passes\n");
        return out.toString();
    }
}
