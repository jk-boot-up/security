package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Runs this project's own Gradle wrapper inside a checkout of the library, so that the build there never touches this project's build directory. */
final class Gradle {

    private Gradle() {
    }

    static List<String> command(String wrapper, String task) {
        return List.of(wrapper, "--console=plain", "-q", task);
    }

    static String wrapper() {
        return Path.of("gradlew").toAbsolutePath().toString();
    }

    static Sh.Result run(Path checkout, String task) throws IOException, InterruptedException {
        return Sh.run(checkout, Map.of(), command(wrapper(), task));
    }

    /** Builds the jar in a checkout and returns it. */
    static Path jar(Path checkout) throws IOException, InterruptedException {
        Sh.Result r = run(checkout, "jar");
        if (!r.ok()) {
            throw new IllegalStateException("gradle jar failed: " + r.output());
        }
        return checkout.resolve("build").resolve("libs").resolve("bank-ledger-1.0.0.jar");
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
