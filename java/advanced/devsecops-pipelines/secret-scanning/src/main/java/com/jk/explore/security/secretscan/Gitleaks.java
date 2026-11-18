package com.jk.explore.security.secretscan;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The real gitleaks command line, asked for a JSON report. Its rules decide the golden output, so the demo uses one pinned
 * release, downloaded from the project's GitHub releases into build/ the first time and checked against its SHA-256.
 */
final class Gitleaks {

    static final String VERSION = "8.30.1";

    // SHA-256 of each release archive (from the release's checksums file), so a changed or substituted download is caught.
    private static final Map<String, String> SHA256 = Map.of(
            "gitleaks_" + VERSION + "_darwin_arm64.tar.gz", "b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5",
            "gitleaks_" + VERSION + "_darwin_x64.tar.gz", "dfe101a4db2255fc85120ac7f3d25e4342c3c20cf749f2c20a18081af1952709",
            "gitleaks_" + VERSION + "_linux_arm64.tar.gz", "e4a487ee7ccd7d3a7f7ec08657610aa3606637dab924210b3aee62570fb4b080",
            "gitleaks_" + VERSION + "_linux_x64.tar.gz", "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb");

    private static final Path HOME = Path.of("build", "gitleaks-" + VERSION);

    private Gitleaks() {
    }

    /** What a run found, and the exit code (1 when there is any leak). */
    record Run(List<Finding> findings, int exit, Path report) {
    }

    /** The release archive for this machine, for example gitleaks_8.30.1_darwin_arm64.tar.gz. */
    static String assetName(String os, String arch) {
        String o = os.toLowerCase(Locale.ROOT).contains("mac") ? "darwin" : "linux";
        String a = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "x64";
        return "gitleaks_" + VERSION + "_" + o + "_" + a + ".tar.gz";
    }

    /** The pinned gitleaks program, downloaded and checked the first time. */
    static Path binary() throws IOException, InterruptedException {
        Path bin = HOME.resolve("gitleaks").toAbsolutePath();
        if (!Files.isExecutable(bin)) {
            Files.createDirectories(HOME);
            String asset = assetName(System.getProperty("os.name"), System.getProperty("os.arch"));
            Path archive = HOME.resolve(asset);
            try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
                HttpResponse<Path> r = client.send(HttpRequest.newBuilder(URI.create("https://github.com/gitleaks/gitleaks/releases/download/v" + VERSION + "/" + asset)).build(),
                        HttpResponse.BodyHandlers.ofFile(archive));
                if (r.statusCode() != 200) {
                    throw new IOException("download of " + asset + " answered " + r.statusCode());
                }
            }
            String actual;
            try {
                actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive)));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
            if (!actual.equals(SHA256.get(asset))) {
                Files.deleteIfExists(archive);
                throw new IOException(asset + " does not match its pinned SHA-256 (got " + actual + ")");
            }
            Sh.must(HOME, Map.of(), List.of("tar", "-xzf", asset, "gitleaks"));
        }
        return bin;
    }

    static Run run(Path repo, String... args) throws IOException, InterruptedException {
        Path report = Files.createTempFile("gitleaks", ".json");
        List<String> command = new ArrayList<>(List.of(binary().toString()));
        command.addAll(List.of(args));
        command.addAll(List.of("--no-banner", "-l", "fatal", "--report-format", "json", "--report-path", report.toString()));
        Sh.Result r = Sh.run(repo, Map.of(), command);
        if (r.exit() > 1) {
            throw new IllegalStateException("gitleaks failed: " + r.output());
        }
        List<Finding> found = new ArrayList<>();
        String json = Files.readString(report).strip();
        if (!json.isEmpty()) {
            for (JsonNode n : new JsonMapper().readTree(json)) {
                found.add(new Finding(n.path("RuleID").asString(), n.path("File").asString(), n.path("Secret").asString() + " " + n.path("Match").asString(),
                        n.path("Commit").asString()));
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
