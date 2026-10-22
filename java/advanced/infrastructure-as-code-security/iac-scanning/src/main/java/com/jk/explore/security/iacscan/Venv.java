package com.jk.explore.security.iacscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A Python virtual environment under build/ with Checkov installed with pip the first time. */
public final class Venv {

    static final String CHECKOV = "3.3.30";

    private final Path dir;

    public Venv(Path build) throws IOException, InterruptedException {
        this.dir = build.toAbsolutePath().resolve("checkov-venv");
        Path marker = dir.resolve("installed-" + CHECKOV);
        if (!Files.exists(marker)) {
            run(List.of("python3", "-m", "venv", dir.toString()));
            run(List.of(dir.resolve("bin/pip").toString(), "install", "--quiet", "checkov==" + CHECKOV));
            Files.writeString(marker, "ok");
        }
    }

    Path bin(String name) {
        return dir.resolve("bin").resolve(name);
    }

    static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(900, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }
}
