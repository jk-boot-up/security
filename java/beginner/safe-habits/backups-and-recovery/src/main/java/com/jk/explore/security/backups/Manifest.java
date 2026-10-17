package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/** A list of every file in a folder with its SHA-256, one "hash  path" line each: the backup's own record of what it should contain. */
record Manifest(SortedMap<String, String> entries) {

    static final String FILE_NAME = "MANIFEST.txt";

    /** What a check found: files missing, files whose content differs, and files that should not be there. */
    record Problems(List<String> missing, List<String> changed, List<String> extra) {
        boolean ok() {
            return missing.isEmpty() && changed.isEmpty() && extra.isEmpty();
        }

        int count() {
            return missing.size() + changed.size() + extra.size();
        }
    }

    static Manifest of(Path dir) throws IOException {
        SortedMap<String, String> entries = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String name = dir.relativize(file).toString().replace('\\', '/');
                if (!name.equals(FILE_NAME)) {
                    entries.put(name, Sha256.of(file));
                }
            }
        }
        return new Manifest(entries);
    }

    String toText() {
        StringBuilder text = new StringBuilder();
        entries.forEach((name, hash) -> text.append(hash).append("  ").append(name).append('\n'));
        return text.toString();
    }

    static Manifest parse(String text) {
        SortedMap<String, String> entries = new TreeMap<>();
        for (String line : text.split("\n")) {
            if (!line.isBlank()) {
                int gap = line.indexOf("  ");
                entries.put(line.substring(gap + 2), line.substring(0, gap));
            }
        }
        return new Manifest(entries);
    }

    /** Compares a folder with this manifest. */
    Problems check(Path dir) throws IOException {
        Manifest actual = of(dir);
        List<String> missing = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        List<String> extra = new ArrayList<>();
        entries.forEach((name, hash) -> {
            String found = actual.entries.get(name);
            if (found == null) {
                missing.add(name);
            } else if (!found.equals(hash)) {
                changed.add(name);
            }
        });
        actual.entries.keySet().stream().filter(name -> !entries.containsKey(name)).forEach(extra::add);
        return new Problems(missing, changed, extra);
    }
}
