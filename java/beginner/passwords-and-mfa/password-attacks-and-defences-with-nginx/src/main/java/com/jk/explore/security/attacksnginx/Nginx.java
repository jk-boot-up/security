package com.jk.explore.security.attacksnginx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** One nginx container with a given configuration, started for a demo and stopped afterwards. */
public class Nginx implements AutoCloseable {

    /** The image is pinned to an exact version, so every run uses the same nginx. */
    public static final String IMAGE = "nginx:1.31.7-alpine";

    private final String name;
    private final Path config;
    private final int port;

    private Nginx(String name, Path config, int port) {
        this.name = name;
        this.config = config;
        this.port = port;
    }

    public int port() {
        return port;
    }

    /** Starts a container with this configuration on a free local port and waits until it answers. */
    public static Nginx start(String configText) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("nginx-demo");
        Path conf = dir.resolve("nginx.conf");
        Files.writeString(conf, configText);
        String name = "attacks-nginx-" + UUID.randomUUID().toString().substring(0, 8);
        run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::80",
                "-v", conf + ":/etc/nginx/nginx.conf:ro", IMAGE));
        String mapping = run(List.of("docker", "port", name, "80/tcp")).trim();
        int port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim());
        Nginx nginx = new Nginx(name, conf, port);
        nginx.awaitReady();
        return nginx;
    }

    private void awaitReady() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        for (int i = 0; i < 100; i++) {
            try {
                HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health")).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("nginx did not start");
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    @Override
    public void close() {
        try {
            run(List.of("docker", "stop", "-t", "1", name));
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // the container was started with --rm, so it goes away on its own
        }
        try {
            Files.deleteIfExists(config);
            Files.deleteIfExists(config.getParent());
        } catch (IOException e) {
            // a leftover temporary file does no harm
        }
    }
}
