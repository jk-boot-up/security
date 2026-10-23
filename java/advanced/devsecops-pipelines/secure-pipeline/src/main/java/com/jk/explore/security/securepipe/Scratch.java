package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Temporary directories and files that the demo makes, all removed at the end (the signing keys among them). */
final class Scratch {

    private static final List<Path> MADE = new ArrayList<>();

    private Scratch() {
    }

    static synchronized Path dir(String prefix) throws IOException {
        Path p = Files.createTempDirectory(prefix);
        MADE.add(p);
        return p;
    }

    static synchronized Path file(String prefix, String suffix) throws IOException {
        Path p = Files.createTempFile(prefix, suffix);
        MADE.add(p);
        return p;
    }

    /** How many of the paths made are still there. */
    static synchronized long remaining() {
        return MADE.stream().filter(Files::exists).count();
    }

    static synchronized void cleanUp() throws IOException {
        for (Path p : MADE) {
            Gradle.deleteTree(p);
        }
        MADE.clear();
    }
}
