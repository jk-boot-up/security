package com.jk.explore.security.csrf;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A small browser that follows the rules a real one follows for cookies and request headers. Pages have logical addresses such
 * as https://bank.example and https://evil.example; every request is really sent to the bank's server on this machine.
 *
 * A modern browser enforces SameSite (treating a cookie with no SameSite as Lax), sends Sec-Fetch-Site, and sends Origin on a POST.
 * A legacy browser ignores SameSite and sends no Fetch Metadata, as browsers did before about 2020.
 */
final class Browser {

    record Cookie(String name, String value, String sameSite, boolean secure) {
    }

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final int port;
    private final boolean legacy;
    private final Map<String, Map<String, Cookie>> jar = new HashMap<>();

    Browser(BankServer server, boolean legacy) {
        this.port = server.port();
        this.legacy = legacy;
    }

    /** The user types an address or uses a bookmark: no page started it. */
    Response visit(String url) throws IOException, InterruptedException {
        return send(null, "GET", url, Map.of(), true, Map.of());
    }

    /** A form on the page at initiator is submitted, by the user or by the page's own script; the browser navigates to the result. */
    Response submit(String initiator, String method, String url, Map<String, String> form) throws IOException, InterruptedException {
        return send(initiator, method, url, form, true, Map.of());
    }

    /** The user clicks a link on the page at initiator. */
    Response click(String initiator, String url) throws IOException, InterruptedException {
        return send(initiator, "GET", url, Map.of(), true, Map.of());
    }

    /** The page's script calls fetch(); it can add headers, but only values it knows. */
    Response fetch(String initiator, String method, String url, Map<String, String> form, Map<String, String> headers)
            throws IOException, InterruptedException {
        return send(initiator, method, url, form, false, headers);
    }

    Optional<Cookie> cookie(String host, String name) {
        return Optional.ofNullable(jar.getOrDefault(host, Map.of()).get(name));
    }

    private Response send(String initiator, String method, String url, Map<String, String> form, boolean topLevel, Map<String, String> extra)
            throws IOException, InterruptedException {
        URI logical = URI.create(url);
        String query = logical.getRawQuery() != null ? "?" + logical.getRawQuery()
                : "GET".equals(method) && !form.isEmpty() ? "?" + encode(form) : "";
        URI wire = URI.create("http://127.0.0.1:" + port + logical.getRawPath() + query);
        HttpRequest.Builder b = HttpRequest.newBuilder(wire);
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.header("Content-Type", "application/x-www-form-urlencoded").method(method, HttpRequest.BodyPublishers.ofString(encode(form)));
        }
        String cookieHeader = cookiesFor(initiator, method, url, topLevel);
        if (!cookieHeader.isEmpty()) {
            b.header("Cookie", cookieHeader);
        }
        if (!legacy) {
            b.header("Sec-Fetch-Site", Site.fetchSite(initiator, url));
            b.header("Sec-Fetch-Mode", topLevel ? "navigate" : "cors");
        }
        if (initiator != null && !"GET".equals(method)) {
            b.header("Origin", initiator);
        }
        extra.forEach(b::header);
        HttpResponse<String> response = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().firstValue("set-cookie").ifPresent(v -> {
            headers.put("Set-Cookie", v);
            store(Site.host(url), v);
        });
        return new Response(response.statusCode(), response.body(), headers);
    }

    private String cookiesFor(String initiator, String method, String url, boolean topLevel) {
        boolean sameSite = initiator == null || Site.sameSite(initiator, url);
        return jar.getOrDefault(Site.host(url), Map.of()).values().stream()
                .filter(c -> legacy || allowed(c, sameSite, method, topLevel))
                .map(c -> c.name() + "=" + c.value())
                .collect(Collectors.joining("; "));
    }

    private static boolean allowed(Cookie c, boolean sameSite, String method, boolean topLevel) {
        String rule = c.sameSite() == null ? "lax" : c.sameSite().toLowerCase(Locale.ROOT);
        return switch (rule) {
            case "strict" -> sameSite;
            case "none" -> c.secure();
            default -> sameSite || (topLevel && "GET".equals(method));
        };
    }

    private void store(String host, String setCookie) {
        String[] parts = setCookie.split(";");
        int eq = parts[0].indexOf('=');
        String sameSite = null;
        boolean secure = false;
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].strip();
            String lower = p.toLowerCase(Locale.ROOT);
            if (lower.equals("secure")) {
                secure = true;
            } else if (lower.startsWith("samesite=")) {
                sameSite = p.substring(9);
            }
        }
        if (!legacy && "none".equalsIgnoreCase(sameSite) && !secure) {
            return;
        }
        Cookie cookie = new Cookie(parts[0].substring(0, eq).strip(), parts[0].substring(eq + 1).strip(), sameSite, secure);
        jar.computeIfAbsent(host, h -> new HashMap<>()).put(cookie.name(), cookie);
    }

    private static String encode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
