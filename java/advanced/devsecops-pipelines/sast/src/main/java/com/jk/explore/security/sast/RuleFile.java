package com.jk.explore.security.sast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads the patterns of one rule out of a Semgrep rule file: what is a source, a sink and a sanitizer. */
record RuleFile(List<String> lines) {

    static RuleFile read(Path file) throws IOException {
        return new RuleFile(Files.readAllLines(file));
    }

    /** The patterns listed under a key ("pattern-sources", "pattern-sinks", "pattern-sanitizers") of one rule. */
    List<String> patterns(String rule, String key) {
        List<String> found = new ArrayList<>();
        boolean inRule = false;
        boolean inKey = false;
        for (String line : lines) {
            String t = line.strip();
            if (t.startsWith("- id: ")) {
                inRule = t.equals("- id: " + rule);
                inKey = false;
            } else if (inRule && t.endsWith(":") && !t.startsWith("-")) {
                inKey = t.equals(key + ":");
            } else if (inRule && inKey && t.startsWith("- pattern: ")) {
                found.add(t.substring("- pattern: ".length()));
            } else if (inRule && !t.startsWith("-")) {
                inKey = false;
            }
        }
        return found;
    }
}
