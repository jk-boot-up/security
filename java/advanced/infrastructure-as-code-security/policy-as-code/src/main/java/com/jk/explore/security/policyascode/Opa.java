package com.jk.explore.security.policyascode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The opa and conftest command line tools, at the pinned versions below (see docs/prerequisites.md). */
public final class Opa {

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** The versions the golden output was made with; another version could decide or count differently. */
    static final String OPA_VERSION = "1.21.3";
    static final String CONFTEST_VERSION = "0.71.1";

    /** Output and exit code of a command. */
    public record Result(int exit, String out) {
    }

    private Opa() {
    }

    static Result run(String stdin, List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (var os = p.getOutputStream()) {
            if (stdin != null) {
                os.write(stdin.getBytes());
            }
        }
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(120, TimeUnit.SECONDS);
        return new Result(p.exitValue(), out);
    }

    /** Stops with a clear message when opa or conftest is not the pinned version. */
    public static void requirePinnedVersions() throws IOException, InterruptedException {
        String opa = run(null, List.of("opa", "version")).out();
        String conftest = run(null, List.of("conftest", "--version")).out();
        if (!opa.contains("Version: " + OPA_VERSION + "\n") || !conftest.contains("Conftest: " + CONFTEST_VERSION + "\n")) {
            throw new IllegalStateException("this demo is pinned to opa " + OPA_VERSION + " and conftest " + CONFTEST_VERSION
                    + "; found:\n" + opa + conftest);
        }
    }

    /** The value of a query (data.steps, ...) with these Rego files loaded, or null when it is undefined. */
    public static JsonNode eval(List<Path> files, String query, JsonNode input) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("opa", "eval", "--stdin-input", "-f", "json"));
        files.forEach(f -> cmd.addAll(List.of("-d", f.toString())));
        cmd.add(query);
        JsonNode results = JSON.readTree(run(input.toString(), cmd).out()).path("result");
        return results.isArray() && !results.isEmpty() ? results.get(0).get("expressions").get(0).get("value") : null;
    }

    /** The messages of data.&lt;pkg&gt;.deny for an input, sorted. Empty means allowed. */
    public static List<String> deny(Path policy, String pkg, JsonNode input) throws IOException, InterruptedException {
        List<String> out = new ArrayList<>();
        JsonNode value = eval(List.of(policy), "data." + pkg + ".deny", input);
        if (value != null) {
            value.forEach(m -> out.add(m.asString()));
        }
        out.sort(null);
        return out;
    }

    public record Tests(int passed, int total) {
    }

    private static final Pattern PASS = Pattern.compile("PASS: (\\d+)/(\\d+)");

    /** Runs opa test on the given paths, only on tests whose name matches the regular expression (null: all). */
    public static Tests test(String regex, List<Path> paths) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("opa", "test"));
        if (regex != null) {
            cmd.addAll(List.of("--run", regex));
        }
        paths.forEach(p -> cmd.add(p.toString()));
        Result r = run(null, cmd);
        Matcher m = PASS.matcher(r.out());
        if (m.find()) {
            return new Tests(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }
        return new Tests(0, r.exit() == 0 ? 0 : -1);
    }

    /** What conftest reported for a folder of manifests: failures and warnings by file name. */
    public record Conftest(int exit, List<String> failures, List<String> warnings) {
    }

    public static Conftest conftest(Path policy, Path manifests) throws IOException, InterruptedException {
        Result r = run(null, List.of("conftest", "test", "--policy", policy.toString(), manifests.toString(), "--output", "json", "--no-color"));
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (JsonNode file : JSON.readTree(r.out())) {
            String name = Path.of(file.get("filename").asString()).getFileName().toString();
            for (JsonNode f : file.path("failures")) {
                failures.add(name + ": " + f.get("msg").asString());
            }
            for (JsonNode w : file.path("warnings")) {
                warnings.add(name + ": " + w.get("msg").asString());
            }
        }
        failures.sort(null);
        warnings.sort(null);
        return new Conftest(r.exit(), failures, warnings);
    }
}
