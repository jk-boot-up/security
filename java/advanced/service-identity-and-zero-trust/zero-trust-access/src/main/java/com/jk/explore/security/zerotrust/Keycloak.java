package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A real Keycloak in Docker (start-dev) with the realm "bank" imported from lab/bank-realm.json. The container is removed when the lab is closed. */
final class Keycloak implements AutoCloseable {

    static final String VERSION = "26.8.1";
    static final String IMAGE = "quay.io/keycloak/keycloak:" + VERSION;
    static final String PASSWORD = "demopassword";

    /** What a login returns: the access token and the refresh token that ends the session. */
    record Tokens(String access, String refresh) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String container = "zt-keycloak-" + ProcessHandle.current().pid();
    private final int port;
    private final HttpClient http = HttpClient.newHttpClient();
    private int userinfoCalls;

    private Keycloak(int port) {
        this.port = port;
    }

    static Keycloak start(Path realmFile) throws IOException, InterruptedException {
        if (!Sh.run("docker", "image", "inspect", IMAGE).ok()) {
            Sh.must(List.of("docker", "pull", IMAGE));
        }
        int port;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = s.getLocalPort();
        }
        Keycloak kc = new Keycloak(port);
        Sh.run("docker", "rm", "-f", kc.container);
        Sh.must(List.of("docker", "run", "-d", "--name", kc.container, "-p", "127.0.0.1:" + port + ":8080",
                "-v", realmFile.toAbsolutePath() + ":/opt/keycloak/data/import/bank-realm.json:ro",
                IMAGE, "start-dev", "--import-realm"));
        for (int i = 0; i < 120; i++) {
            try {
                if (kc.http.send(HttpRequest.newBuilder(URI.create(kc.issuer() + "/.well-known/openid-configuration")).build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return kc;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(2000);
        }
        kc.close();
        throw new IllegalStateException("keycloak did not start");
    }

    String issuer() {
        return "http://localhost:" + port + "/realms/bank";
    }

    private URI endpoint(String name) {
        return URI.create(issuer() + "/protocol/openid-connect/" + name);
    }

    Map<String, PublicKey> signingKeys() throws IOException, InterruptedException, GeneralSecurityException {
        return Jwks.parse(http.send(HttpRequest.newBuilder(endpoint("certs")).build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    Tokens login(String client, String user) throws IOException, InterruptedException {
        HttpResponse<String> res = post("token", Map.of("grant_type", "password", "client_id", client, "username", user,
                "password", PASSWORD, "scope", "openid"));
        JsonNode body = JSON.readTree(res.body());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("login of " + user + " on " + client + " failed: " + body.path("error_description").asText());
        }
        return new Tokens(body.path("access_token").asText(), body.path("refresh_token").asText());
    }

    /** Ends the user's session at Keycloak (the refresh token names it). */
    void logout(String client, Tokens tokens) throws IOException, InterruptedException {
        int status = post("logout", Map.of("client_id", client, "refresh_token", tokens.refresh())).statusCode();
        if (status != 204) {
            throw new IllegalStateException("logout failed: " + status);
        }
    }

    /** The session check: does Keycloak still know a live session behind this token? */
    synchronized boolean sessionActive(String accessToken) {
        userinfoCalls++;
        try {
            return http.send(HttpRequest.newBuilder(endpoint("userinfo")).header("Authorization", "Bearer " + accessToken).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    synchronized int sessionChecks() {
        return userinfoCalls;
    }

    private HttpResponse<String> post(String endpoint, Map<String, String> form) throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return http.send(HttpRequest.newBuilder(endpoint(endpoint)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Stops the container, as an outage would. */
    void stop() throws IOException, InterruptedException {
        Sh.must(List.of("docker", "stop", "-t", "1", container));
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", container);
        } catch (IOException e) {
            // nothing more to do
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
