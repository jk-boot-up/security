package com.jk.explore.security.sast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The real Semgrep command line, run with rules from files, no account and no metrics. */
final class Semgrep {

    private final Venv venv;

    Semgrep(Venv venv) {
        this.venv = venv;
    }

    /** What a scan found, and the exit code (1 with --error when there is a finding). */
    record Scan(List<Finding> findings, int exit) {
    }

    Scan scan(Path code, boolean error, String... rules) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(venv.bin("semgrep").toString()));
        for (String r : rules) {
            command.addAll(List.of("--config", r));
        }
        command.addAll(List.of("--json", "--metrics=off", "--disable-version-check", "--quiet"));
        if (error) {
            command.add("--error");
        }
        command.add(code.toString());
        Sh.Result r = Sh.run(command);
        String out = r.output();
        int start = out.indexOf('{');
        List<Finding> found = new ArrayList<>();
        for (JsonNode n : new JsonMapper().readTree(out.substring(start)).path("results")) {
            String id = n.path("check_id").asString();
            found.add(new Finding(id.substring(id.lastIndexOf('.') + 1), Path.of(n.path("path").asString()).getFileName().toString(), n.path("start").path("line").asInt()));
        }
        found.sort(java.util.Comparator.comparing(Finding::file).thenComparingInt(Finding::line).thenComparing(Finding::rule));
        return new Scan(found, r.exit());
    }

    /** One check of semgrep --test: a rule, the lines its test file expects it to flag, and the lines it flagged. */
    record Check(String rule, boolean passed, List<Integer> expected, List<Integer> reported) {
    }

    /** The checks of a test run, sorted by rule, and how many rule files have no test file. */
    record TestRun(List<Check> checks, List<String> missingTests) {

        long passed() {
            return checks.stream().filter(Check::passed).count();
        }
    }

    /** semgrep --test on a folder of rule files and their test files. */
    TestRun test(Path rules) throws IOException, InterruptedException {
        Sh.Result r = Sh.run(venv.bin("semgrep").toString(), "--test", "--json", "--metrics=off", "--disable-version-check", rules.toString());
        return parseTest(r.output().substring(r.output().indexOf('{')));
    }

    /** Reads the JSON that semgrep --test prints. */
    static TestRun parseTest(String json) {
        JsonNode root = new JsonMapper().readTree(json);
        List<Check> checks = new ArrayList<>();
        for (JsonNode file : root.path("results")) {
            for (var check : file.path("checks").properties()) {
                List<Integer> expected = new ArrayList<>();
                List<Integer> reported = new ArrayList<>();
                for (JsonNode m : check.getValue().path("matches")) {
                    m.path("expected_lines").forEach(n -> expected.add(n.asInt()));
                    m.path("reported_lines").forEach(n -> reported.add(n.asInt()));
                }
                checks.add(new Check(check.getKey(), check.getValue().path("passed").asBoolean(), expected, reported));
            }
        }
        checks.sort(java.util.Comparator.comparing(Check::rule));
        List<String> missing = new ArrayList<>();
        root.path("config_missing_tests").forEach(n -> missing.add(Path.of(n.asString()).getFileName().toString()));
        return new TestRun(checks, missing);
    }

    static Path copy(Path from) throws IOException {
        Path to = Files.createTempDirectory("code");
        try (var files = Files.walk(from)) {
            for (Path p : files.filter(Files::isRegularFile).toList()) {
                Path target = to.resolve(from.relativize(p));
                Files.createDirectories(target.getParent());
                Files.copy(p, target);
            }
        }
        return to;
    }
}
