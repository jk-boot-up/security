package com.jk.explore.security.bffspring;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * One Keycloak in a Docker container, bound to the loopback address on a free port, with the bank realm imported from {@code infra/bank-realm.json} and removed on close.
 * The realm file names the BFF's address, so the BFF's port is chosen first and written into a temporary copy.
 */
public final class KeycloakLab implements AutoCloseable {

    /** Pinned to an exact version, so every run uses the same Keycloak. */
    static final String IMAGE = "quay.io/keycloak/keycloak:26.8.1";
    static final String ADMIN_PASSWORD = "admin-demo-password";

    private final String name;
    private final int port;
    private final Path imports;
    private final HttpClient http = HttpClient.newBuilder().build();

    private KeycloakLab(String name, int port, Path imports) {
        this.name = name;
        this.port = port;
        this.imports = imports;
    }

    public static KeycloakLab start(int bffPort) throws IOException, InterruptedException {
        String name = "bff-keycloak-" + UUID.randomUUID().toString().substring(0, 8);
        Path imports = Files.createTempDirectory("bff-realms");
        Files.writeString(imports.resolve("bank-realm.json"), Files.readString(Path.of("infra", "bank-realm.json")).replace("${BFF_PORT}", Integer.toString(bffPort)));
        run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::8080", "-e", "KC_BOOTSTRAP_ADMIN_USERNAME=admin", "-e", "KC_BOOTSTRAP_ADMIN_PASSWORD=" + ADMIN_PASSWORD,
                "-v", imports.toAbsolutePath() + ":/opt/keycloak/data/import:ro", IMAGE, "start-dev", "--import-realm"));
        KeycloakLab lab = null;
        try {
            String mapping = run(List.of("docker", "port", name, "8080/tcp")).lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
            lab = new KeycloakLab(name, Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim()), imports);
            lab.awaitReady();
            return lab;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (lab != null) {
                lab.close();
            } else {
                remove(name);
                deleteTree(imports);
            }
            throw e;
        }
    }

    String base() {
        return "http://127.0.0.1:" + port;
    }

    String issuer() {
        return base() + "/realms/bank";
    }

    private void awaitReady() throws IOException, InterruptedException {
        for (int i = 0; i < 240; i++) {
            try {
                if (http.send(HttpRequest.newBuilder(URI.create(issuer() + "/.well-known/openid-configuration")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Keycloak did not start");
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(120, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    private static void remove(String name) {
        try {
            run(List.of("docker", "rm", "-f", name));
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // already gone
        }
    }

    private static void deleteTree(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // a temporary folder left behind is harmless
        }
    }

    @Override
    public void close() {
        remove(name);
        deleteTree(imports);
    }
}
