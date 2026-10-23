package com.jk.explore.security.sast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The comments that switch a rule off for a line. A good one says which rule, why, and who owns the decision. */
public final class Suppressions {

    private static final Pattern ANY = Pattern.compile("nosemgrep");
    private static final Pattern GOOD = Pattern.compile("nosemgrep: [\\w.-]+ -- .{10,} \\(owner: [\\w-]+\\)");

    /** How many suppressions the files have and how many of them are well written. */
    public record Count(int total, int valid) {
    }

    public static Count count(Path dir) throws IOException {
        int total = 0;
        int valid = 0;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(p)) {
                    if (ANY.matcher(line).find()) {
                        total++;
                        Matcher m = GOOD.matcher(line);
                        if (m.find()) {
                            valid++;
                        }
                    }
                }
            }
        }
        return new Count(total, valid);
    }

    /** Adds a suppression comment on its own line before the line number given (1-based). */
    public static List<String> insertBefore(List<String> lines, int line, String comment) {
        List<String> out = new ArrayList<>(lines);
        out.add(line - 1, comment);
        return out;
    }
}
