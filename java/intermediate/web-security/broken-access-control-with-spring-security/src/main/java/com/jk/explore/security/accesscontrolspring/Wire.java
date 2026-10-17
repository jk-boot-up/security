package com.jk.explore.security.accesscontrolspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A tiny HTTP client for the demo: one request, signed in with the demo password or not signed in at all. */
final class Wire {

    record Reply(int status, String body) {
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private Wire() {
    }

    static Reply call(int port, String method, String path, String user) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (user != null) {
            String credentials = Base64.getEncoder().encodeToString((user + ":" + Catalogue.PASSWORD).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + credentials);
        }
        HttpResponse<String> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }
}
