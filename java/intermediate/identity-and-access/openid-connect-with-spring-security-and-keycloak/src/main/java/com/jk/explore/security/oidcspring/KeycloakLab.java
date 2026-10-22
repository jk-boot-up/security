package com.jk.explore.security.oidcspring;

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
 * One Keycloak in a Docker container, bound to the loopback address on a free port, with the bank and wallet realms imported from {@code infra/} and removed on close.
 * The realm files name the store's address, so the store's port is chosen first and written into a temporary copy. Keycloak reaches the store's back-channel logout
 * address through {@code host.docker.internal}, which on Docker Desktop leads to the host's loopback interface.
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

    public static KeycloakLab start(int storePort) throws IOException, InterruptedException {
        String name = "oidc-keycloak-" + UUID.randomUUID().toString().substring(0, 8);
        Path imports = Files.createTempDirectory("oidc-realms");
        for (String realm : List.of("bank", "wallet")) {
            Files.writeString(imports.resolve(realm + "-realm.json"), Files.readString(Path.of("infra", realm + "-realm.json")).replace("${STORE_PORT}", Integer.toString(storePort)));
        }
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

    String issuer(String realm) {
        return base() + "/realms/" + realm;
    }

    private void awaitReady() throws IOException, InterruptedException {
        for (String realm : List.of("bank", "wallet")) {
            for (int i = 0; i < 240; i++) {
                try {
                    if (http.send(HttpRequest.newBuilder(URI.create(issuer(realm) + "/.well-known/openid-configuration")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                        break;
                    }
                } catch (IOException e) {
                    // not up yet
                }
                if (i == 239) {
                    throw new IllegalStateException("Keycloak did not start");
                }
                Thread.sleep(500);
            }
        }
    }

    private String adminToken() throws IOException, InterruptedException {
        HttpResponse<String> token = http.send(HttpRequest.newBuilder(URI.create(base() + "/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=password&client_id=admin-cli&username=admin&password=" + URLEncoder.encode(ADMIN_PASSWORD, StandardCharsets.UTF_8))).build(),
                HttpResponse.BodyHandlers.ofString());
        return java.util.regex.Pattern.compile("\"access_token\":\"([^\"]+)\"").matcher(token.body()).results().map(m -> m.group(1)).findFirst().orElseThrow();
    }

    /** Ends one Keycloak session through the admin API, which makes Keycloak send the back-channel logout message to the store. */
    int endSession(String realm, String sessionId) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(base() + "/admin/realms/" + realm + "/sessions/" + sessionId)).header("Authorization", "Bearer " + adminToken()).DELETE().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** Adds a second RSA signing key with a higher priority, so that new tokens are signed with it and both keys are published. */
    int addSigningKey(String realm) throws IOException, InterruptedException {
        String body = "{\"name\":\"rsa-new\",\"providerId\":\"rsa-generated\",\"providerType\":\"org.keycloak.keys.KeyProvider\","
                + "\"config\":{\"priority\":[\"200\"],\"enabled\":[\"true\"],\"active\":[\"true\"],\"keySize\":[\"2048\"]}}";
        return http.send(HttpRequest.newBuilder(URI.create(base() + "/admin/realms/" + realm + "/components")).header("Authorization", "Bearer " + adminToken())
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** The realm's published key set, as JSON. */
    String certs(String realm) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(issuer(realm) + "/protocol/openid-connect/certs")).build(), HttpResponse.BodyHandlers.ofString()).body();
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
