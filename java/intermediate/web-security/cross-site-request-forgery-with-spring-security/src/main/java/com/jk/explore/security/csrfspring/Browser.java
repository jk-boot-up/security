package com.jk.explore.security.csrfspring;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A customer's browser as far as this lesson needs one: it keeps cookies per site and attaches them to every request to that site,
 * whichever page caused the request. That is the whole of the weakness. A request from evil.example is the same request, plus an Origin header.
 */
final class Browser {

    record Reply(int status, String body, List<String> setCookies) {

        /** The status and the bank's words. Spring's error page carries a time stamp, so only its reason is kept. */
        String text() {
            var reason = java.util.regex.Pattern.compile("\"error\":\"([^\"]+)\"").matcher(body);
            return status + " " + (reason.find() ? reason.group(1) : body.strip());
        }
    }

    private final int port;
    private final CookieManager cookies = new CookieManager();
    private final HttpClient client;

    Browser(int port) {
        this.port = port;
        this.client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    Reply get(String path, String... headers) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).GET(), headers);
    }

    Reply post(String path, Map<String, String> form, String... headers) throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)), headers);
    }

    /** The same request when another site's page caused it: the browser adds the cookie and says where the page came from. */
    Reply postFromAnotherSite(String path, Map<String, String> form, String... headers) throws IOException, InterruptedException {
        String[] all = new String[headers.length + 2];
        System.arraycopy(headers, 0, all, 0, headers.length);
        all[headers.length] = "Origin";
        all[headers.length + 1] = "https://evil.example";
        return post(path, form, all);
    }

    Reply getFromAnotherSite(String path) throws IOException, InterruptedException {
        return get(path, "Origin", "https://evil.example");
    }

    /** The form token the bank's own page would carry, read from the bank's pages (which only the bank's own origin may do). */
    String formToken() throws IOException, InterruptedException {
        return get("/csrf").body();
    }

    /** What the bank's page script reads from the cookie jar in single-page mode. */
    HttpCookie cookie(String name) {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    Reply signIn(String user, String... headers) throws IOException, InterruptedException {
        Map<String, String> form = new LinkedHashMap<>(Map.of("username", user, "password", WebSecurity.PASSWORD));
        return post("/login", form, headers);
    }

    /** The API client: credentials in a header on every request, no cookies kept. */
    static Reply apiPost(int port, String user, String path, Map<String, String> form) throws IOException, InterruptedException {
        String body = form.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        if (user != null) {
            request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + WebSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), response.headers().allValues("set-cookie"));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private Reply send(HttpRequest.Builder request, String... headers) throws IOException, InterruptedException {
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), response.headers().allValues("set-cookie"));
    }
}
