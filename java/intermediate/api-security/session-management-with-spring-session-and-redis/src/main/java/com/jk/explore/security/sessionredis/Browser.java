package com.jk.explore.security.sessionredis;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A customer's device as far as this lesson needs one: it keeps the session cookie the bank sets and sends it back to every page. It reads the
 * Set-Cookie header itself, so it keeps a cookie that a real browser would also keep over https.
 */
final class Browser {

    static final String COOKIE = "__Host-session";

    record Reply(int status, String body, List<String> setCookies) {

        /** The status and the first line of the bank's answer. */
        String text() {
            return status + (body.isBlank() ? "" : " " + body.lines().findFirst().orElse(""));
        }
    }

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private String baseUrl;
    private String cookie;

    Browser(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Points this device at another server, as a load balancer does when it sends the next request elsewhere. */
    Browser at(String otherBaseUrl) {
        Browser copy = new Browser(otherBaseUrl);
        copy.cookie = cookie;
        return copy;
    }

    String cookie() {
        return cookie;
    }

    void setCookie(String value) {
        this.cookie = value;
    }

    Reply get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    Reply post(String path, String form) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)));
    }

    Reply signIn(String user, String password) throws IOException, InterruptedException {
        return post("/login", "username=" + URLEncoder.encode(user, StandardCharsets.UTF_8) + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8));
    }

    Reply signIn(String user) throws IOException, InterruptedException {
        return signIn(user, BankSecurity.PASSWORD);
    }

    private Reply send(HttpRequest.Builder request) throws IOException, InterruptedException {
        if (cookie != null) {
            request.header("Cookie", COOKIE + "=" + cookie);
        }
        HttpResponse<String> r = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        List<String> set = r.headers().allValues("set-cookie");
        for (String header : set) {
            if (header.startsWith(COOKIE + "=")) {
                String value = header.substring(COOKIE.length() + 1, header.indexOf(';') < 0 ? header.length() : header.indexOf(';'));
                cookie = value.isEmpty() ? null : value;   // an empty value is the bank telling the browser to forget the cookie
            }
        }
        return new Reply(r.statusCode(), r.body(), set);
    }
}
