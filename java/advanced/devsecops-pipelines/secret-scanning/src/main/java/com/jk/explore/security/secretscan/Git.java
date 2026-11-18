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

    /** The commit ids, oldest first, so a commit can be named by its number (1, 2, ...). */
    List<String> ids() throws IOException, InterruptedException {
        return List.of(must("rev-list", "--reverse", "--all").strip().split("\n"));
    }

    /** One line added or removed by a commit. */
    record Change(int commit, boolean added, String file, String line) {
    }

    /** Every line any commit added or removed, with the commit's number. */
    List<Change> changes() throws IOException, InterruptedException {
        List<String> ids = ids();
        List<Change> changes = new ArrayList<>();
        int commit = 0;
        String file = "";
        for (String line : must("log", "--all", "-p", "--format=commit %H", "-U0").split("\n")) {
            if (line.startsWith("commit ")) {
                commit = ids.indexOf(line.substring(7)) + 1;
            } else if (line.startsWith("--- a/") || line.startsWith("+++ b/")) {
                file = line.substring(6);
            } else if ((line.startsWith("+") || line.startsWith("-")) && !line.startsWith("+++") && !line.startsWith("---")) {
                changes.add(new Change(commit, line.startsWith("+"), file, line.substring(1)));
            }
        }
        return changes;
    }

    int commits() throws IOException, InterruptedException {
        return Integer.parseInt(must("rev-list", "--count", "--all").strip());
    }
}
