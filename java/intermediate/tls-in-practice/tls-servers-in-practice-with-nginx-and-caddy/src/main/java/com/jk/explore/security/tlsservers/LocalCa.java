package com.jk.explore.security.tlsservers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** A root made with the step command, and certificates it signs directly; nginx has no CA of its own. */
final class LocalCa {

    private final Path dir;

    LocalCa(Path dir) throws IOException, InterruptedException {
        this.dir = Files.createDirectories(dir);
        Procs.run(List.of("step", "certificate", "create", "Shop Lab Root", root().toString(), key("root").toString(), "--profile", "root-ca", "--no-password", "--insecure", "--force"));
    }

    Path root() {
        return dir.resolve("root.crt");
    }

    Path key(String name) {
        return dir.resolve(name + ".key");
    }

    Path cert(String name) {
        return dir.resolve(name + ".crt");
    }

    /** A server certificate for {@code names}, signed by the root, lasting {@code lifetime} (for example 24h). */
    void issue(String file, String lifetime, String... names) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>(List.of("step", "certificate", "create", names[0], cert(file).toString(), key(file).toString(), "--profile", "leaf", "--ca", root().toString(),
                "--ca-key", key("root").toString(), "--no-password", "--insecure", "--not-after", lifetime, "--force"));
        for (String name : names) {
            command.addAll(List.of("--san", name));
        }
        Procs.run(command);
    }
}
