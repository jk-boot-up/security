package com.jk.explore.security.failclosedspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** A plain HTTP client for the loopback bank. */
final class Wire {

    record Reply(int status, String body) {

        /** The reference in a refusal, or an empty string. */
        String reference() {
            var m = java.util.regex.Pattern.compile("REF-\\d{4}").matcher(body);
            return m.find() ? m.group() : "";
        }
    }

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    Reply pay(String bankUrl, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(bankUrl + "/payments")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body());
    }
}
