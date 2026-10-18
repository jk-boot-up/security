package com.jk.explore.security.asvs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Requirements kept as a plain tab-separated file next to the code, so they are reviewed and versioned like code. */
final class RequirementsFile {

    static final String HEADER = "id\tchapter\tlevel\tverification\tcheck\tedition\tstatement";

    private RequirementsFile() {
    }

    static void write(Path file, List<Requirement> requirements) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Requirement r : requirements) {
            lines.add(String.join("\t", r.id(), r.chapter(), r.level().name(), r.verification().name(), r.check(), r.edition(), r.statement()));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    static List<Requirement> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
            throw new IOException("not a requirements file: the header is missing");
        }
        List<Requirement> requirements = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] f = lines.get(i).split("\t", -1);
            if (f.length != 7) {
                throw new IOException("line " + (i + 1) + " has " + f.length + " fields, not 7");
            }
            try {
                requirements.add(new Requirement(f[0], f[1], Level.valueOf(f[2]), Verification.valueOf(f[3]), f[4], f[5], f[6]));
            } catch (IllegalArgumentException e) {
                throw new IOException("line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(requirements);
    }
}
