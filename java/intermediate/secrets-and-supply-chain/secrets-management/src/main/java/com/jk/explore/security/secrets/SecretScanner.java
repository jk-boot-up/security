package com.jk.explore.security.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds secrets written into files, the way repository and image scanners do: by the name of the setting
 * (password, secret, api key, token) and by how random a long value looks (its Shannon entropy).
 */
final class SecretScanner {

    record Finding(String file, int line, String rule) {
        @Override
        public String toString() {
            return file + ":" + line + " (" + rule + ")";
        }
    }

    private static final Pattern NAMED = Pattern.compile(
            "(?i)\\b([a-z0-9_.-]*(password|passwd|secret|api[_.-]?key|token)[a-z0-9_.-]*)\\s*[:=]\\s*\"?([^\"\\s#]{8,})");
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9+/_=-]{24,}");
    static final double ENTROPY_THRESHOLD = 4.0;

    private SecretScanner() {
    }

    static List<Finding> scan(Path root) throws IOException {
        List<Finding> findings = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String name = root.relativize(file).toString();
                findings.addAll(scanText(name, Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
        return findings;
    }

    static List<Finding> scanText(String name, String text) {
        List<Finding> findings = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher named = NAMED.matcher(line);
            if (named.find() && !isPlaceholder(named.group(3))) {
                findings.add(new Finding(name, i + 1, "a setting named " + named.group(1).toLowerCase(Locale.ROOT)));
                continue;
            }
            Matcher word = WORD.matcher(line);
            while (word.find()) {
                if (entropy(word.group()) >= ENTROPY_THRESHOLD) {
                    findings.add(new Finding(name, i + 1, "a random-looking value"));
                    break;
                }
            }
        }
        return findings;
    }

    /** Values such as ${DB_PASSWORD} or file references are where a secret is read from, not the secret itself. */
    static boolean isPlaceholder(String value) {
        return value.startsWith("${") || value.startsWith("file:") || value.equalsIgnoreCase("changeme-at-runtime");
    }

    /** Shannon entropy in bits per character. */
    static double entropy(String s) {
        int[] counts = new int[128];
        int other = 0;
        for (char c : s.toCharArray()) {
            if (c < 128) {
                counts[c]++;
            } else {
                other++;
            }
        }
        double h = 0;
        for (int n : counts) {
            if (n > 0) {
                double p = (double) n / s.length();
                h -= p * Math.log(p) / Math.log(2);
            }
        }
        if (other > 0) {
            double p = (double) other / s.length();
            h -= p * Math.log(p) / Math.log(2);
        }
        return h;
    }
}
