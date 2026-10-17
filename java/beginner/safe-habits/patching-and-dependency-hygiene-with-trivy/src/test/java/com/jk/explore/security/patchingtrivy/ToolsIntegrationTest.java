package com.jk.explore.security.patchingtrivy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Independent checks with real tools: Trivy (in Docker) scans what this code writes, and a nested Gradle build refuses a changed checksum.
 * Run with ./gradlew integrationTest. The first Trivy run downloads its vulnerability database, so it needs the network once;
 * the database is then kept in the Docker volume trivy-cache-patching. Without Docker or the database the Trivy tests are skipped.
 */
@Tag("tools")
class ToolsIntegrationTest {

    static final String IMAGE = "aquasec/trivy:0.76.0";
    static final String CACHE = "trivy-cache-patching";

    @TempDir
    Path dir;

    record Result(int exit, String output) {
    }

    private static Result run(List<String> command, Path workingDirectory, long minutes) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")));
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        Process process = builder.start();
        Thread reader = null;
        StringBuilder output = new StringBuilder();
        reader = Thread.ofPlatform().start(() -> {
            try {
                output.append(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // the process was stopped
            }
        });
        if (!process.waitFor(minutes, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            reader.join(5000);
            return new Result(-1, output.toString());
        }
        reader.join(5000);
        return new Result(process.exitValue(), output.toString());
    }

    private static boolean dockerAvailable() {
        try {
            return run(List.of("docker", "info"), null, 1).exit() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static boolean databaseReady;

    /** Downloads the vulnerability database once, so that each scan can run with --skip-db-update and never waits for the network. */
    private static void prepareDatabase() throws IOException, InterruptedException {
        if (databaseReady) {
            return;
        }
        String name = "trivy-db-" + UUID.randomUUID();
        Result result = run(List.of("docker", "run", "--rm", "--name", name, "-v", CACHE + ":/root/.cache", IMAGE, "--quiet", "image", "--download-db-only", "--no-progress"), null, 10);
        if (result.exit() != 0) {
            run(List.of("docker", "rm", "-f", name), null, 1);
            assumeTrue(false, "the vulnerability database could not be downloaded: " + result.output().lines().limit(3).collect(Collectors.joining(" | ")));
        }
        databaseReady = true;
    }

    /** Runs `trivy <arguments>` in a container that is removed afterwards, and returns the JSON report written to the output folder. */
    private String trivy(Path scanned, String... arguments) throws IOException, InterruptedException {
        assumeTrue(dockerAvailable(), "Docker is not available");
        prepareDatabase();
        Path out = Files.createDirectories(dir.resolve("out-" + UUID.randomUUID()));
        String name = "trivy-test-" + UUID.randomUUID();
        List<String> command = new ArrayList<>(List.of("docker", "run", "--rm", "--name", name,
                "-v", scanned.toAbsolutePath() + ":/work:ro", "-v", out.toAbsolutePath() + ":/out", "-v", CACHE + ":/root/.cache", IMAGE, "--quiet"));
        command.add(arguments[0]);
        command.add("--skip-db-update");
        command.addAll(List.of(arguments).subList(1, arguments.length));
        Result result = run(command, null, 8);
        if (result.exit() != 0) {
            run(List.of("docker", "rm", "-f", name), null, 1);
            assumeTrue(false, "Trivy could not run, perhaps the vulnerability database could not be downloaded: " + result.output().lines().limit(5).collect(Collectors.joining(" | ")));
        }
        return Files.readString(out.resolve("report.json"));
    }

    private Path writeSbom(List<Dependency> dependencies) throws IOException {
        Path folder = Files.createDirectories(dir.resolve("sbom-" + UUID.randomUUID()));
        Files.writeString(folder.resolve("online-store.cdx.json"), CycloneDx.write("online-store", "1.0.0", dependencies));
        return folder;
    }

    private static Set<String> ids(List<Vulnerability> found) {
        return found.stream().map(v -> v.id() + " " + v.packageName() + " " + v.installed()).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    @Test
    void trivyReadsTheSbomThisCodeWritesAndAgreesWithOurOwnChecks() throws Exception {
        List<Dependency> locked = GradleLockfile.parse(Files.readString(Path.of("fixtures", "gradle.lockfile")));
        Path folder = writeSbom(locked);
        String json = trivy(folder, "sbom", "--no-progress", "--format", "json", "--output", "/out/report.json", "/work/online-store.cdx.json");
        List<Vulnerability> found = TrivyReport.parse(json);
        assertFalse(found.isEmpty());
        Set<String> known = locked.stream().map(Dependency::coordinates).collect(Collectors.toSet());
        for (Vulnerability v : found) {
            assertTrue(known.contains(v.packageName() + ":" + v.installed()), "Trivy reported a library that is not in the bill of materials: " + v);
            assertTrue(v.nearestFix().isPresent() || v.fixed().isBlank(), v.toString());
        }
        for (OwnAdvisory advisory : OwnAdvisory.known()) {
            assertTrue(found.stream().anyMatch(v -> v.id().equals(advisory.id())), "Trivy missed " + advisory.id());
        }
    }

    @Test
    void trivyReadsTheGradleLockfileAndFindsTheSameProblemsAsTheSbom() throws Exception {
        List<Dependency> locked = GradleLockfile.parse(Files.readString(Path.of("fixtures", "gradle.lockfile")));
        String fromSbom = trivy(writeSbom(locked), "sbom", "--no-progress", "--format", "json", "--output", "/out/report.json", "/work/online-store.cdx.json");
        String fromLock = trivy(Path.of("fixtures"), "fs", "--no-progress", "--scanners", "vuln", "--format", "json", "--output", "/out/report.json", "/work/gradle.lockfile");
        assertEquals(ids(TrivyReport.parse(fromSbom)), ids(TrivyReport.parse(fromLock)));
    }

    @Test
    void upgradingToTheTargetsRemovesTheCriticalProblems() throws Exception {
        List<Vulnerability> before = TrivyReport.parse(Files.readString(Path.of("fixtures", "trivy-report.json")));
        Map<String, Optional<Version>> targets = Triage.upgradeTargets(before);
        List<Dependency> upgraded = new ArrayList<>();
        for (Dependency d : GradleLockfile.parse(Files.readString(Path.of("fixtures", "gradle.lockfile")))) {
            upgraded.add(targets.containsKey(d.name()) && targets.get(d.name()).isPresent()
                    ? new Dependency(d.group(), d.artifact(), targets.get(d.name()).get().toString()) : d);
        }
        String json = trivy(writeSbom(upgraded), "sbom", "--no-progress", "--format", "json", "--output", "/out/report.json", "/work/online-store.cdx.json");
        Set<String> remaining = TrivyReport.parse(json).stream().map(Vulnerability::id).collect(Collectors.toSet());
        Set<String> critical = new HashSet<>();
        before.stream().filter(v -> v.severity().equals("CRITICAL")).forEach(v -> critical.add(v.id()));
        critical.retainAll(remaining);
        assertEquals(Set.of(), critical, "critical problems that are still there after the upgrade");
    }

    private Result gradle(Path project) throws IOException, InterruptedException {
        Path wrapper = Path.of("gradlew").toAbsolutePath();
        return run(List.of(wrapper.toString(), "-q", "-p", project.toString(), "resolveAll"), Path.of("").toAbsolutePath(), 5);
    }

    private Path copyVerifiedBuild() throws IOException {
        Path copy = Files.createDirectories(dir.resolve("verified-build"));
        Path source = Path.of("fixtures", "verified-build");
        try (var files = Files.walk(source)) {
            for (Path file : files.toList()) {
                Path target = copy.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
        return copy;
    }

    @Test
    void gradleBuildsWhenTheChecksumsMatch() throws Exception {
        Result result = gradle(copyVerifiedBuild());
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("commons-codec-1.23.0.jar"));
    }

    @Test
    void gradleRefusesToBuildWhenOneChecksumIsChanged() throws Exception {
        Path project = copyVerifiedBuild();
        Path metadata = project.resolve("gradle").resolve("verification-metadata.xml");
        Files.writeString(metadata, Files.readString(metadata).replace("47504a3ce71d73a1b0af07675546b8d8c5af87f82ef12b6381f9eb3c17dd5a5b", "0".repeat(64)));
        Result result = gradle(project);
        assertTrue(result.exit() != 0, "the build should have failed");
        assertTrue(result.output().contains("Dependency verification failed"), result.output());
    }
}
