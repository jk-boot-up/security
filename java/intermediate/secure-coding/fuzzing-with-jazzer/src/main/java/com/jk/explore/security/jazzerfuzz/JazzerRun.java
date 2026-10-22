package com.jk.explore.security.jazzerfuzz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The real Jazzer, started as a program of its own on one fuzz target, with a corpus of seed inputs and a fixed random seed, and its report read back. */
public final class JazzerRun {

    /** What to run: the target, the seed inputs, how many runs, the random seed, and the longest input. */
    public record Settings(String target, List<String> seeds, int runs, int randomSeed, int maxLen) {
    }

    /** What came out: the distinct findings, the runs done and the coverage points reached. */
    public record Result(List<Finding> findings, int runs, int coverage) {
    }

    private static final Pattern DONE = Pattern.compile("^#(\\d+)\\s+DONE\\s+cov: (\\d+)");
    private static final Pattern FRAME = Pattern.compile("^\\s+at (com\\.jk\\.explore\\.security\\.jazzerfuzz\\.(?!targets\\.)([A-Za-z]+)\\.([A-Za-z<>]+))\\(([A-Za-z]+\\.java):(\\d+)\\)");

    private JazzerRun() {
    }

    public static Result run(Settings settings) throws IOException, InterruptedException {
        Path work = Files.createTempDirectory("jazzer-run");
        Path corpus = Files.createDirectories(work.resolve("corpus"));
        int i = 0;
        for (String seed : settings.seeds()) {
            Files.writeString(corpus.resolve("seed" + i++), seed);
        }
        Path artifacts = Files.createDirectories(work.resolve("artifacts"));
        List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-XX:-OmitStackTraceInFastThrow", "-cp", System.getProperty("java.class.path"),
                "com.code_intelligence.jazzer.Jazzer", "--target_class=com.jk.explore.security.jazzerfuzz.targets." + settings.target(), "--instrumentation_includes=com.jk.explore.security.jazzerfuzz.**",
                "--keep_going=20", "-runs=" + settings.runs(), "-seed=" + settings.randomSeed(), "-max_len=" + settings.maxLen(), "-artifact_prefix=" + artifacts + "/", corpus.toString()));
        Process p = new ProcessBuilder(command).redirectErrorStream(true).directory(work.toFile()).start();
        String output = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("jazzer did not finish");
        }
        return parse(output);
    }

    /** Reads the report text: each "== Java Exception" starts a finding, the first frame in the store's code says where, the DEDUP_TOKEN line ends it. */
    static Result parse(String output) {
        Map<String, Finding> byToken = new LinkedHashMap<>();
        String exception = null;
        String where = null;
        String token = null;
        int runs = 0;
        int coverage = 0;
        List<String> pendingTokens = new ArrayList<>();
        for (String line : output.split("\n")) {
            if (line.startsWith("== Java Exception: ")) {
                exception = simpleName(line.substring("== Java Exception: ".length()));
                where = null;
            } else if (exception != null && where == null) {
                Matcher f = FRAME.matcher(line);
                if (f.find()) {
                    where = f.group(2) + "." + f.group(3) + " (line " + f.group(5) + ")";
                }
            }
            if (line.startsWith("DEDUP_TOKEN: ") && exception != null) {
                token = line.substring("DEDUP_TOKEN: ".length()).strip();
                if (where != null) {
                    String kind = exception;
                    String place = where;
                    if (byToken.values().stream().noneMatch(f -> f.exception().equals(kind) && f.where().equals(place))) {
                        byToken.put(token, new Finding(kind, place, token, null));
                    }
                    pendingTokens.add(token);
                }
                exception = null;
            }
            if (line.contains("Test unit written to ") && !pendingTokens.isEmpty()) {
                String path = line.substring(line.indexOf("Test unit written to ") + "Test unit written to ".length()).strip();
                Finding f = byToken.get(pendingTokens.remove(0));
                if (f != null && f.input() == null) {
                    byToken.put(f.token(), new Finding(f.exception(), f.where(), f.token(), Path.of(path)));
                }
            }
            Matcher d = DONE.matcher(line);
            if (d.find()) {
                runs = Integer.parseInt(d.group(1));
                coverage = Integer.parseInt(d.group(2));
            }
        }
        return new Result(new ArrayList<>(byToken.values()), runs, coverage);
    }

    /** "java.lang.ArrayIndexOutOfBoundsException: Index 1 ..." becomes "ArrayIndexOutOfBoundsException"; a Jazzer security issue becomes its name, such as "Regular Expression Injection". */
    private static String simpleName(String text) {
        if (text.contains("FuzzerSecurityIssue")) {
            return text.substring(text.indexOf(':') + 1).strip();
        }
        String className = text.contains(":") ? text.substring(0, text.indexOf(':')) : text;
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
