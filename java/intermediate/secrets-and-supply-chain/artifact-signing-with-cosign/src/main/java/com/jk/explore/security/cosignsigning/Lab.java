package com.jk.explore.security.cosignsigning;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A container registry on a free loopback port, a folder for key pairs, and the real images of the lesson. The "release" is nginx 1.31.7 and the "attacker's build" is nginx 1.31.6;
 * the other builds are other real images that are already on this machine. Each is pushed to the local registry under the shop's name; a signature belongs to the digest of the content. Needs Docker, the registry:3 image and the cosign command.
 */
public final class Lab implements AutoCloseable {

    static final String REGISTRY_IMAGE = "registry:3";
    static final String RELEASE_IMAGE = "nginx:1.31.7-alpine";
    static final String ATTACKER_IMAGE = "nginx:1.31.6-alpine";
    static final String BETA_IMAGE = "nginx:1.31.5-alpine";
    static final String THIEF_IMAGE = "nats:2.15.0-alpine";
    static final String LAPTOP_IMAGE = "redis:8.10.2-alpine";
    static final String UNSIGNED_IMAGE = "alpine:3.17";
    static final String PASSPHRASE = "lab-passphrase";

    private final String container;
    private final int port;
    private final Path keys;
    private final HttpClient http = HttpClient.newHttpClient();

    private Lab(String container, int port, Path keys) {
        this.container = container;
        this.port = port;
        this.keys = keys;
    }

    public static Lab start() throws IOException, InterruptedException {
        String name = "cosign-registry-" + UUID.randomUUID().toString().substring(0, 8);
        int port = freePort();
        run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1:" + port + ":5000", REGISTRY_IMAGE), null);
        try {
            Lab lab = new Lab(name, port, Files.createTempDirectory("cosign-keys"));
            lab.awaitReady();
            return lab;
        } catch (IOException | InterruptedException | RuntimeException e) {
            run(List.of("docker", "rm", "-f", name), null);
            throw e;
        }
    }

    /**
     * A free port in the 15000s. Docker's own push connects to a registry on the loopback address from inside Docker Desktop's virtual machine, which reaches the ports
     * published there but did not reach the high ports that the system hands out at random, so the lab chooses a low one.
     */
    private static int freePort() throws IOException {
        for (int port = 15000; port < 15900; port++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port, 1, java.net.InetAddress.getLoopbackAddress())) {
                return port;
            } catch (IOException e) {
                // in use, try the next
            }
        }
        throw new IOException("no free port between 15000 and 15900");
    }

    String registry() {
        return "127.0.0.1:" + port;
    }

    Path keys() {
        return keys;
    }

    private void awaitReady() throws IOException, InterruptedException {
        for (int i = 0; i < 60; i++) {
            try {
                if (http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v2/")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("the registry did not start");
    }

    /** Pushes a local image under {@code name:tag} (replacing what the tag pointed at) and returns the repository with the digest that the registry reports. */
    String push(String localImage, String name, String tag) throws IOException, InterruptedException {
        String ref = registry() + "/" + name + ":" + tag;
        run(List.of("docker", "tag", localImage, ref), null);
        String output = pushWithRetry(ref);
        Matcher m = Pattern.compile("digest: (sha256:[0-9a-f]{64})").matcher(output);
        if (!m.find()) {
            throw new IllegalStateException("no digest in " + output);
        }
        return registry() + "/" + name + "@" + m.group(1);
    }

    /** Docker's own connection to a registry on the loopback address can lag behind the published port by a few seconds, so a refused first push is tried again. */
    private String pushWithRetry(String ref) throws IOException, InterruptedException {
        IllegalStateException last = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return run(List.of("docker", "push", ref), null);
            } catch (IllegalStateException e) {
                last = e;
                Thread.sleep(500);
            }
        }
        throw last;
    }

    /** The digest that a tag points at right now, as a registry answers it. */
    String resolve(String name, String tag) throws IOException, InterruptedException {
        HttpResponse<Void> r = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v2/" + name + "/manifests/" + tag))
                .header("Accept", "application/vnd.oci.image.index.v1+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json, application/vnd.docker.distribution.manifest.list.v2+json")
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
        return registry() + "/" + name + "@" + r.headers().firstValue("Docker-Content-Digest").orElseThrow();
    }

    /** The tags of a repository, the signatures' tags included. */
    String tags(String name) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v2/" + name + "/tags/list")).build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    @Override
    public void close() {
        try {
            run(List.of("docker", "rm", "-f", container), null);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // already gone
        }
        try (Stream<Path> files = Files.walk(keys)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // a temporary folder left behind is harmless
        }
    }

    static String run(List<String> command, Path directory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        Process p = builder.start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(180, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }
}
