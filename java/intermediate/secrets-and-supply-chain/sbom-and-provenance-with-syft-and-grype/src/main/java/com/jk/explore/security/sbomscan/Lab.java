package com.jk.explore.security.sbomscan;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The images of the lesson, built on the machine from a base image that is already here and a few library jars from Maven Central, and catalogued and scanned with the real
 * syft and grype commands. The jars are downloaded once and kept in the build folder.
 */
public final class Lab {

    static final String BASE = "eclipse-temurin:21-jre-alpine";
    static final String OLD = "2.14.1";
    static final String NEW = "2.25.5";
    static final String REPOSITORY = "https://repo1.maven.org/maven2/org/apache/logging/log4j/";

    private final Path jars;
    private final Path work;

    public Lab() throws IOException {
        this.jars = Files.createDirectories(Path.of("build", "jars"));
        this.work = Files.createTempDirectory("sbomscan");
    }

    Path work() {
        return work;
    }

    /** Builds shop-sbom:{@code tag} with log4j-core and log4j-api of the given version in /app/lib (as .jar files, or all under one name when {@code renamedTo} is given). */
    String build(String tag, String version, String renamedTo) throws IOException, InterruptedException {
        Path context = Files.createDirectories(work.resolve("ctx-" + tag));
        StringBuilder dockerfile = new StringBuilder("FROM " + BASE + "\n");
        for (String artifact : List.of("log4j-core", "log4j-api")) {
            Path jar = download(artifact, version);
            String name = renamedTo != null && artifact.equals("log4j-core") ? renamedTo : jar.getFileName().toString();
            if (renamedTo != null && artifact.equals("log4j-api")) {
                continue;
            }
            Files.copy(jar, context.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            dockerfile.append("COPY ").append(name).append(" /app/lib/\n");
        }
        Files.writeString(context.resolve("Dockerfile"), dockerfile.toString());
        run(List.of("docker", "build", "-q", "-t", "shop-sbom:" + tag, context.toString()));
        return "shop-sbom:" + tag;
    }

    private Path download(String artifact, String version) throws IOException, InterruptedException {
        Path target = jars.resolve(artifact + "-" + version + ".jar");
        if (!Files.exists(target)) {
            HttpResponse<Path> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(REPOSITORY + artifact + "/" + version + "/" + artifact + "-" + version + ".jar")).build(),
                    HttpResponse.BodyHandlers.ofFile(target));
            if (r.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IllegalStateException("could not download " + target.getFileName() + ": " + r.statusCode());
            }
        }
        return target;
    }

    /** The image id, which changes whenever the image does. */
    String imageId(String image) throws IOException, InterruptedException {
        return run(List.of("docker", "image", "inspect", "--format", "{{.Id}}", image)).strip();
    }

    static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(false).start();
        String out = new String(p.getInputStream().readAllBytes());
        String err = new String(p.getErrorStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + err + out);
        }
        return out;
    }
}
