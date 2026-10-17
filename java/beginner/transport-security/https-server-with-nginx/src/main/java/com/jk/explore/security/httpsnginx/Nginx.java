package com.jk.explore.security.httpsnginx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** One nginx container with a given configuration and the demo certificates, started for a demo and stopped afterwards. */
public class Nginx implements AutoCloseable {

    /** The image is pinned to an exact version, so every run uses the same nginx. */
    public static final String IMAGE = "nginx:1.31.7-alpine";

    private final String name;
    private final Path dir;
    private final int httpPort;
    private final int httpsPort;

    private Nginx(String name, Path dir, int httpPort, int httpsPort) {
        this.name = name;
        this.dir = dir;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    public int httpPort() {
        return httpPort;
    }

    public int httpsPort() {
        return httpsPort;
    }

    private static Path prepare(String configText) throws IOException {
        Path dir = Files.createTempDirectory("nginx-https-demo");
        Files.writeString(dir.resolve("nginx.conf"), configText);
        Path pki = Files.createDirectory(dir.resolve("pki"));
        Pki.copyForNginx(pki);
        return dir;
    }

    public static Nginx start(NginxConfig.Variant variant) throws IOException, InterruptedException {
        Path dir = prepare(NginxConfig.text(variant));
        String name = "https-nginx-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::80", "-p", "127.0.0.1::443",
                    "-v", dir.resolve("nginx.conf") + ":/etc/nginx/nginx.conf:ro", "-v", dir.resolve("pki") + ":/pki:ro", IMAGE));
            Nginx nginx = new Nginx(name, dir, port(name, "80/tcp"), port(name, "443/tcp"));
            nginx.awaitReady();
            return nginx;
        } catch (IOException | InterruptedException | RuntimeException e) {
            stop(name);
            delete(dir);
            throw e;
        }
    }

    /** Runs `nginx -t` on a configuration without starting a server, and returns what it said, ending with "exit code N". */
    public static String configTest(String configText) throws IOException, InterruptedException {
        Path dir = prepare(configText);
        try {
            Process p = new ProcessBuilder("docker", "run", "--rm", "-v", dir.resolve("nginx.conf") + ":/etc/nginx/nginx.conf:ro",
                    "-v", dir.resolve("pki") + ":/pki:ro", IMAGE, "nginx", "-t").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IllegalStateException("nginx -t did not finish");
            }
            return out + "exit code " + p.exitValue();
        } finally {
            delete(dir);
        }
    }

    private static int port(String name, String inside) throws IOException, InterruptedException {
        String mapping = run(List.of("docker", "port", name, inside)).trim().lines().findFirst().orElseThrow();
        return Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim());
    }

    private void awaitReady() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        for (int i = 0; i < 100; i++) {
            try {
                HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + httpPort + "/health")).build(),
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

    private static void stop(String name) {
        try {
            run(List.of("docker", "stop", "-t", "1", name));
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // the container was started with --rm, so it goes away on its own
        }
    }

    private static void delete(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            List<Path> all = new ArrayList<>(files.sorted(Comparator.reverseOrder()).toList());
            for (Path p : all) {
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            // a leftover temporary file does no harm
        }
    }

    @Override
    public void close() {
        stop(name);
        delete(dir);
    }
}
