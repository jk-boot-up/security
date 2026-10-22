package com.jk.explore.security.oauthkeycloak;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * One Keycloak in a Docker container, bound to the loopback address on a free port, with the bank realm imported from {@code infra/bank-realm.json}
 * and removed on close. Needs Docker and the {@value #IMAGE} image (pulled on first use).
 */
public final class KeycloakLab implements AutoCloseable {

    /** Pinned to an exact version, so every run uses the same Keycloak. */
    static final String IMAGE = "quay.io/keycloak/keycloak:26.8.1";

    private final String name;
    private final int port;

    private KeycloakLab(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public static KeycloakLab start() throws IOException, InterruptedException {
        String name = "oauth-keycloak-" + UUID.randomUUID().toString().substring(0, 8);
        Path realm = Path.of("infra", "bank-realm.json").toAbsolutePath();
        run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::8080",
                "-e", "KC_BOOTSTRAP_ADMIN_USERNAME=admin", "-e", "KC_BOOTSTRAP_ADMIN_PASSWORD=admin-demo-password",
                "-v", realm + ":/opt/keycloak/data/import/bank-realm.json:ro", IMAGE, "start-dev", "--import-realm"));
        KeycloakLab lab = null;
        try {
            String mapping = run(List.of("docker", "port", name, "8080/tcp")).lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
            lab = new KeycloakLab(name, Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim()));
            lab.awaitReady();
            return lab;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (lab != null) {
                lab.close();
            } else {
                remove(name);
            }
            throw e;
        }
    }

    /** The address of Keycloak, without the realm. */
    String base() {
        return "http://127.0.0.1:" + port;
    }

    /** The issuer of the bank realm: what every token's {@code iss} must equal. */
    String issuer() {
        return base() + "/realms/bank";
    }

    private void awaitReady() throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().build();
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

    @Override
    public void close() {
        remove(name);
    }
}
