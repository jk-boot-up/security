package com.jk.explore.security.secretscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A git repository driven through the git command line, with fixed author and dates so that runs do not differ. */
final class Git {

    static final Map<String, String> ENV = Map.of("GIT_AUTHOR_NAME", "dev", "GIT_AUTHOR_EMAIL", "dev@bank.example", "GIT_COMMITTER_NAME", "dev",
            "GIT_COMMITTER_EMAIL", "dev@bank.example", "GIT_AUTHOR_DATE", "2026-01-05T10:00:00", "GIT_COMMITTER_DATE", "2026-01-05T10:00:00");

    final Path dir;

    Git(Path dir) {
        this.dir = dir;
    }

    static Git init() throws IOException, InterruptedException {
        Git g = new Git(Files.createTempDirectory("repo"));
        g.must("init", "-q", "-b", "main");
        return g;
    }

    Sh.Result run(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        return Sh.run(dir, ENV, command);
    }

    String must(String... args) throws IOException, InterruptedException {
        Sh.Result r = run(args);
        if (!r.ok()) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + r.output());
        }
        return r.output();
    }

    void write(String file, String text) throws IOException {
        Path p = dir.resolve(file);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text);
    }

    void commit(String message) throws IOException, InterruptedException {
        must("add", "-A");
        must("commit", "-q", "-m", message);
    }

    int commits() throws IOException, InterruptedException {
        return Integer.parseInt(must("rev-list", "--count", "--all").strip());
    }
}
