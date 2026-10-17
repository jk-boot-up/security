package com.jk.explore.security.authkeycloak;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One Keycloak container with the shop realm imported, started for the integration tests and removed afterwards. */
final class Keycloak implements AutoCloseable {

    /** Pinned to an exact version, so every run uses the same Keycloak. */
    static final String IMAGE = "quay.io/keycloak/keycloak:26.8.1";
    static final String REDIRECT = "http://127.0.0.1:9/callback";

    private final String name;
    private final int port;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private Keycloak(String name, int port) {
        this.name = name;
        this.port = port;
    }

    static Keycloak start() throws IOException, InterruptedException {
        String name = "auth-keycloak-" + UUID.randomUUID().toString().substring(0, 8);
        Path realm = Path.of("infra", "shop-realm.json").toAbsolutePath();
        run(List.of("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::8080",
                "-e", "KC_BOOTSTRAP_ADMIN_USERNAME=admin", "-e", "KC_BOOTSTRAP_ADMIN_PASSWORD=admin-demo-password",
                "-v", realm + ":/opt/keycloak/data/import/shop-realm.json:ro",
                IMAGE, "start-dev", "--import-realm"));
        int port;
        try {
            String mapping = run(List.of("docker", "port", name, "8080/tcp")).trim().split("\n")[0];
            port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).trim());
        } catch (IOException | InterruptedException | RuntimeException e) {
            remove(name);
            throw e;
        }
        Keycloak keycloak = new Keycloak(name, port);
        try {
            keycloak.awaitReady();
        } catch (IOException | InterruptedException | RuntimeException e) {
            keycloak.close();
            throw e;
        }
        return keycloak;
    }

    String issuer() {
        return "http://127.0.0.1:" + port + "/realms/shop";
    }

    String base() {
        return "http://127.0.0.1:" + port;
    }

    private void awaitReady() throws IOException, InterruptedException {
        for (int i = 0; i < 240; i++) {
            try {
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(issuer() + "/.well-known/openid-configuration")).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Keycloak did not start");
    }

    /** A token for a confidential client with no user, using the client-credentials grant. */
    String clientCredentials(String client, String secret) throws IOException, InterruptedException {
        return token(post(issuer() + "/protocol/openid-connect/token",
                "grant_type=client_credentials&client_id=" + client + "&client_secret=" + secret));
    }

    /** A token for a user, by driving the login page as a browser would: authorization code with PKCE (S256). */
    String login(String user, String password) throws IOException, InterruptedException {
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        // Keycloak marks its login cookies Secure. A browser still sends them to 127.0.0.1 over http, because loopback counts as
        // a secure context; Java's cookie manager does not, so this small cookie jar plays the browser's part.
        Map<String, String> jar = new LinkedHashMap<>();
        HttpClient browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        String authorize = issuer() + "/protocol/openid-connect/auth?client_id=shop-web&response_type=code&scope=openid&state=s1"
                + "&redirect_uri=" + enc(REDIRECT) + "&code_challenge=" + challenge + "&code_challenge_method=S256";
        HttpResponse<String> page = browser.send(HttpRequest.newBuilder(URI.create(authorize)).build(), HttpResponse.BodyHandlers.ofString());
        remember(jar, page);
        Matcher form = Pattern.compile("id=\"kc-form-login\"[^>]*action=\"([^\"]+)\"").matcher(page.body());
        if (!form.find()) {
            throw new IllegalStateException("no login form: " + page.statusCode());
        }
        String action = form.group(1).replace("&amp;", "&");
        HttpResponse<String> submitted = browser.send(HttpRequest.newBuilder(URI.create(action))
                .header("Content-Type", "application/x-www-form-urlencoded").header("Cookie", cookieHeader(jar))
                .POST(HttpRequest.BodyPublishers.ofString("username=" + enc(user) + "&password=" + enc(password) + "&credentialId=")).build(),
                HttpResponse.BodyHandlers.ofString());
        String location = submitted.headers().firstValue("Location").orElseThrow(() -> new IllegalStateException("login refused: " + submitted.statusCode()));
        Matcher code = Pattern.compile("[?&]code=([^&]+)").matcher(location);
        if (!code.find()) {
            throw new IllegalStateException("no code in " + location);
        }
        return token(post(issuer() + "/protocol/openid-connect/token",
                "grant_type=authorization_code&client_id=shop-web&code=" + code.group(1) + "&redirect_uri=" + enc(REDIRECT) + "&code_verifier=" + verifier));
    }

    private static void remember(Map<String, String> jar, HttpResponse<?> response) {
        for (String header : response.headers().allValues("Set-Cookie")) {
            String pair = header.split(";", 2)[0];
            int eq = pair.indexOf('=');
            jar.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
    }

    private static String cookieHeader(Map<String, String> jar) {
        StringBuilder text = new StringBuilder();
        jar.forEach((k, v) -> text.append(text.length() == 0 ? "" : "; ").append(k).append('=').append(v));
        return text.toString();
    }

    /** True if the login page refuses this user name and password. */
    boolean loginRefused(String user, String password) throws IOException, InterruptedException {
        try {
            login(user, password);
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** Takes the realm role away from a user through the admin API. */
    void removeRealmRole(String user, String role) throws IOException, InterruptedException {
        String adminToken = token(post(base() + "/realms/master/protocol/openid-connect/token",
                "grant_type=password&client_id=admin-cli&username=admin&password=admin-demo-password"));
        String users = adminGet("/admin/realms/shop/users?username=" + user + "&exact=true", adminToken);
        String id = first(users, "\"id\":\"([^\"]+)\"");
        String roleJson = adminGet("/admin/realms/shop/roles/" + role, adminToken);
        String roleId = first(roleJson, "\"id\":\"([^\"]+)\"");
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(base() + "/admin/realms/shop/users/" + id + "/role-mappings/realm"))
                .header("Authorization", "Bearer " + adminToken).header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString("[{\"id\":\"" + roleId + "\",\"name\":\"" + role + "\"}]")).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            throw new IllegalStateException("role not removed: " + response.statusCode() + " " + response.body());
        }
    }

    private String adminGet(String path, String adminToken) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Authorization", "Bearer " + adminToken).build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    private String post(String url, String form) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("token request failed: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private static String token(String json) {
        return first(json, "\"access_token\":\"([^\"]+)\"");
    }

    private static String first(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) {
            throw new IllegalStateException("not found: " + regex + " in " + text);
        }
        return m.group(1);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
