package com.jk.explore.security.leastprivilegespring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A tiny HTTP client for the demo: one request with optional basic credentials. */
final class Wire {

    record Reply(int status, String body) {
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private Wire() {
    }

    static Reply call(int port, String method, String path, String user, String password) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (user != null) {
            String token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + token);
        }
        HttpResponse<String> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    static Reply call(int port, String method, String path, String user) throws IOException, InterruptedException {
        return call(port, method, path, user, Catalogue.PASSWORD);
    }
}
