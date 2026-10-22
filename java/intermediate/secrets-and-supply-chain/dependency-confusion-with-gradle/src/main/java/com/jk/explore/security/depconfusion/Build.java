package com.jk.explore.security.depconfusion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** One real Gradle build of a small consumer project, in a folder of its own, using this project's Gradle wrapper. */
final class Build {

    /** The outcome: the dependencies that were resolved (group:name:version from origin) or a short reason the build failed. */
    record Result(List<String> resolved, String failure, String output) {

        boolean ok() {
            return failure.isEmpty();
        }

        boolean resolvedFrom(String coordinates, String origin) {
            return resolved.contains(coordinates + " from " + origin);
        }

        @Override
        public String toString() {
            return ok() ? String.join(", ", resolved) : "refused: " + failure;
        }
    }

    private final Path dir;
    private final String repositories;
    private final String dependencies;

    /**
     * @param repositories the Groovy inside {@code repositories { }} in settings.gradle, in the order the build asks them
     * @param dependencies the Groovy inside {@code dependencies { }}
     */
    Build(Path dir, String repositories, String dependencies) throws IOException {
        this.dir = Files.createDirectories(dir);
        this.repositories = repositories;
        this.dependencies = dependencies;
        Files.writeString(dir.resolve("settings.gradle"), "rootProject.name = 'consumer'\n"
                + "dependencyResolutionManagement {\n    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS\n    repositories {\n" + repositories + "\n    }\n}\n");
        Files.writeString(dir.resolve("build.gradle"), """
                configurations { cp { canBeResolved = true; canBeConsumed = false } }
                dependencies {
                %s
                }
                tasks.register('show') {
                    def files = configurations.cp.incoming.artifactView { }.artifacts
                    doLast {
                        files.artifacts.each { a ->
                            def id = a.id.componentIdentifier
                            def origin = new java.util.zip.ZipFile(a.file).withCloseable { z -> z.getInputStream(z.getEntry('origin.txt')).text }
                            println "RESOLVED ${id.group}:${id.module}:${id.version} from ${origin}"
                        }
                    }
                }
                """.formatted(dependencies));
    }

    Path dir() {
        return dir;
    }

    Result run(String... extra) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(Path.of("gradlew").toAbsolutePath().toString(), "-p", dir.toString(), "--console=plain", "-q", "--no-build-cache", "--refresh-dependencies"));
        command.addAll(List.of(extra));
        command.add("show");
        Process p = new ProcessBuilder(command).start();
        String out = new String(p.getInputStream().readAllBytes());
        String err = new String(p.getErrorStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("gradle did not finish");
        }
        List<String> resolved = out.lines().filter(l -> l.startsWith("RESOLVED ")).map(l -> l.substring("RESOLVED ".length())).sorted().toList();
        return new Result(resolved, p.exitValue() == 0 ? "" : reason(err + out), err + out);
    }

    /** Gradle's long error text, reduced to one sentence per kind of failure. */
    String reason(String text) {
        if (text.contains("Dependency verification failed")) {
            return verification(text);
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Could not find ([^\\s:]+:[^\\s:]+:\\d[\\d.]*\\d)").matcher(text);
        if (m.find()) {
            return "could not find " + m.group(1);
        }
        return "build failed";
    }

    /** Gradle's message names the artifact; whether the metadata lists that version tells the two failures apart. */
    private String verification(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^\\s:]+):([^\\s:]+):([^\\s:)]+)\\)").matcher(text);
        try {
            String metadata = Files.readString(dir.resolve("gradle").resolve("verification-metadata.xml"));
            if (m.find() && metadata.contains("name=\"" + m.group(2) + "\" version=\"" + m.group(3) + "\"")) {
                return "checksum differs from gradle/verification-metadata.xml";
            }
        } catch (IOException e) {
            // no metadata file: nothing is listed
        }
        return "not listed in gradle/verification-metadata.xml";
    }
}
