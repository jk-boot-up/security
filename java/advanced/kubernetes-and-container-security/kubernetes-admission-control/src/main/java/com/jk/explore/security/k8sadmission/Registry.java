package com.jk.explore.security.k8sadmission;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A local image registry (a container on kind's Docker network), the bank's cosign key, and images pushed and signed in it. */
public final class Registry implements AutoCloseable {

    private final String container = "bank-registry-" + UUID.randomUUID().toString().substring(0, 6);
    private final int hostPort;
    private final String clusterHost;
    private final Path keys;

    public Registry() throws IOException, InterruptedException {
        this.hostPort = freePort();
        Sh.must(List.of("docker", "run", "-d", "--name", container, "--network", "kind", "-p", "127.0.0.1:" + hostPort + ":5000", "registry:2"));
        this.clusterHost = Sh.must(List.of("docker", "inspect", container, "--format", "{{(index .NetworkSettings.Networks \"kind\").IPAddress}}")).strip() + ":5000";
        this.keys = Files.createTempDirectory("bank-keys");
        Sh.must(keys, Map.of("COSIGN_PASSWORD", ""), List.of("cosign", "generate-key-pair"));
        waitUntilUp();
    }

    /** A free port in the 5000s: the Docker daemon on a Mac cannot push to a port in the ephemeral range that it chose itself. */
    private static int freePort() throws IOException {
        for (int port = 5100; port < 5900; port++) {
            try (java.net.ServerSocket s = new java.net.ServerSocket(port, 1, java.net.InetAddress.getLoopbackAddress())) {
                return port;
            } catch (IOException busy) {
                continue;
            }
        }
        throw new IOException("no free port");
    }

    private void waitUntilUp() throws IOException, InterruptedException {
        for (int i = 0; i < 50; i++) {
            try {
                if (HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + hostPort + "/v2/")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                Thread.sleep(200);
            }
        }
        throw new IllegalStateException("the registry did not start");
    }

    /** How the cluster names the registry. */
    public String clusterHost() {
        return clusterHost;
    }

    public String publicKey() throws IOException {
        return Files.readString(keys.resolve("cosign.pub"));
    }

    /** Builds a one-layer image with the given text in it, pushes it as {@code repo:1} and {@code repo:latest}, and returns its digest. */
    public String push(String repo, String content) throws IOException, InterruptedException {
        Path ctx = Files.createTempDirectory("image");
        Files.writeString(ctx.resolve("Dockerfile"), "FROM alpine:3.24.3\nRUN echo '" + content + "' > /id\n");
        String ref = "localhost:" + hostPort + "/" + repo + ":1";
        Sh.must(List.of("docker", "build", "-q", "-t", ref, ctx.toString()));
        Sh.must(List.of("docker", "push", "-q", ref));
        String latest = "localhost:" + hostPort + "/" + repo + ":latest";
        Sh.must(List.of("docker", "tag", ref, latest));
        Sh.must(List.of("docker", "push", "-q", latest));
        HttpResponse<Void> head = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + hostPort + "/v2/" + repo + "/manifests/1"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json").build(), HttpResponse.BodyHandlers.discarding());
        return head.headers().firstValue("Docker-Content-Digest").orElseThrow();
    }

    /** Signs an image by digest with the bank's key. The signature is stored in the registry next to the image. */
    public void sign(String repo, String digest) throws IOException, InterruptedException {
        Sh.must(keys, Map.of("COSIGN_PASSWORD", ""), List.of("cosign", "sign", "--key", "cosign.key", "--allow-insecure-registry", "--use-signing-config=false", "--tlog-upload=false", "--yes",
                "localhost:" + hostPort + "/" + repo + "@" + digest));
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", container);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
