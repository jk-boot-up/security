package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.List;

/** Reads a Gradle dependency lock file (gradle.lockfile): one `group:artifact:version=configurations` line per resolved library. */
final class GradleLockfile {

    private GradleLockfile() {
    }

    static List<Dependency> parse(String text) {
        List<Dependency> dependencies = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("empty=")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            String[] parts = (equals < 0 ? trimmed : trimmed.substring(0, equals)).split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("not a lock file line: " + line);
            }
            dependencies.add(new Dependency(parts[0], parts[1], parts[2]));
        }
        return dependencies;
    }
}
