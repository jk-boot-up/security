package com.jk.explore.security.secretscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** A small secret scanner written for this project: a few patterns and an entropy check, on files or on every line ever added. */
public final class Detector {

    record Rule(String name, Pattern pattern) {
    }

    static final List<Rule> RULES = List.of(
            new Rule("cloud-access-key", Pattern.compile("AKIA[0-9A-Z]{16}")),
            new Rule("payment-live-key", Pattern.compile("sk_live_[A-Za-z0-9]{20,}")),
            new Rule("source-token", Pattern.compile("gh[pousr]_[A-Za-z0-9]{36}")),
            new Rule("password-assignment", Pattern.compile("(?i)(?:password|passwd|secret)\\s*[=:]\\s*(\\S{6,})")));

    /** Bits per character from which a long word counts as a secret; hex (16 symbols, at most 4 bits) never reaches it. */
    static final double THRESHOLD = 4.2;

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9+/_=-]{24,}");

    /** Shannon entropy in bits per character. */
    static double entropy(String s) {
        int[] counts = new int[256];
        for (char c : s.toCharArray()) {
            counts[c & 0xff]++;
        }
        double h = 0;
        for (int n : counts) {
            if (n > 0) {
                double p = (double) n / s.length();
                h -= p * Math.log(p) / Math.log(2);
            }
        }
        return h;
    }

    public List<Finding> scanText(String file, String text) {
        List<Finding> found = new ArrayList<>();
        for (Rule r : RULES) {
            Matcher m = r.pattern().matcher(text);
            while (m.find()) {
                found.add(new Finding(r.name(), file, m.group()));
            }
        }
        Matcher w = WORD.matcher(text);
        while (w.find()) {
            String word = w.group();
            boolean alreadyNamed = found.stream().anyMatch(f -> f.text().contains(word));
            if (!alreadyNamed && entropy(word) >= THRESHOLD) {
                found.add(new Finding("high-entropy-string", file, word));
            }
        }
        return found;
    }

    /** The files of a working tree (not the .git folder). */
    public List<Finding> scanTree(Path root) throws IOException {
        List<Finding> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(Files::isRegularFile).filter(f -> !root.relativize(f).startsWith(".git")).sorted().toList()) {
                found.addAll(scanText(root.relativize(p).toString(), Files.readString(p)));
            }
        }
        return found;
    }

    /** Every line that any commit added, whether or not it is still there. */
    List<Finding> scanHistory(Git repo) throws IOException, InterruptedException {
        List<Finding> found = new ArrayList<>();
        String file = "";
        String commit = "";
        for (String line : repo.must("log", "--all", "-p", "--format=commit %H", "-U0").split("\n")) {
            if (line.startsWith("commit ")) {
                commit = line.substring(7);
            } else if (line.startsWith("+++ b/")) {
                file = line.substring(6);
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                for (Finding f : scanText(file, line.substring(1))) {
                    found.add(new Finding(f.rule(), f.file(), f.text(), commit));
                }
            }
        }
        return found;
    }
}
