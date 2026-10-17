package com.jk.explore.security.xssspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/** Fetches a page from a running store over the loopback interface with the JDK's HTTP client. */
final class Wire {

    record Page(int status, String body, String policy, List<String> cookies) {
    }

    private Wire() {
    }

    static Page get(int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return new Page(response.statusCode(), response.body(),
                response.headers().firstValue("Content-Security-Policy").orElse(null),
                response.headers().allValues("Set-Cookie"));
    }
}
