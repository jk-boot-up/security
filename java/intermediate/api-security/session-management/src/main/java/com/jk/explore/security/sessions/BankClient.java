package com.jk.explore.security.sessions;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** A real HTTP client that keeps its cookies in a {@link Browser}, so the session cookie is handled the way the demo describes. */
final class BankClient implements AutoCloseable {

    record Reply(int status, String setCookie) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;
    final Browser browser = new Browser();

    BankClient(int port) {
        base = "http://127.0.0.1:" + port;
    }

    Reply signIn(String user, String password) throws IOException, InterruptedException {
        String body = "user=" + URLEncoder.encode(user, StandardCharsets.UTF_8) + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        return send(request("/login").header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    Reply account() throws IOException, InterruptedException {
        return send(request("/account").GET());
    }

    Reply signOut() throws IOException, InterruptedException {
        return send(request("/logout").POST(HttpRequest.BodyPublishers.noBody()));
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        browser.header().ifPresent(cookie -> builder.header("Cookie", cookie));
        return builder;
    }

    private Reply send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        String setCookie = response.headers().firstValue("Set-Cookie").orElse(null);
        if (setCookie != null) {
            browser.receive(setCookie);
        }
        return new Reply(response.statusCode(), setCookie);
    }

    @Override
    public void close() {
        http.close();
    }
}
