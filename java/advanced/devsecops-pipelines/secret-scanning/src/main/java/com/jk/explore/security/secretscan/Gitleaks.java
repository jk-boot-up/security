package com.jk.explore.security.secretscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The real gitleaks command line (brew install gitleaks), asked for a JSON report. */
final class Gitleaks {

    private Gitleaks() {
    }

    /** What a run found, and the exit code (1 when there is any leak). */
    record Run(List<Finding> findings, int exit, Path report) {
    }

    static Run run(Path repo, String... args) throws IOException, InterruptedException {
        Path report = Files.createTempFile("gitleaks", ".json");
        List<String> command = new ArrayList<>(List.of("gitleaks"));
        command.addAll(List.of(args));
        command.addAll(List.of("--no-banner", "-l", "fatal", "--report-format", "json", "--report-path", report.toString()));
        Sh.Result r = Sh.run(repo, java.util.Map.of(), command);
        if (r.exit() > 1) {
            throw new IllegalStateException("gitleaks failed: " + r.output());
        }
        List<Finding> found = new ArrayList<>();
        String json = Files.readString(report).strip();
        if (!json.isEmpty()) {
            for (JsonNode n : new JsonMapper().readTree(json)) {
                found.add(new Finding(n.path("RuleID").asString(), n.path("File").asString(), n.path("Secret").asString() + " " + n.path("Match").asString()));
            }
        }
        return new Run(found, r.exit(), report);
    }

    static Run history(Path repo, String... extra) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("git", repo.toString()));
        args.addAll(List.of(extra));
        return run(repo, args.toArray(String[]::new));
    }

    static Run files(Path repo) throws IOException, InterruptedException {
        return run(repo, "dir", repo.toString());
    }
}
