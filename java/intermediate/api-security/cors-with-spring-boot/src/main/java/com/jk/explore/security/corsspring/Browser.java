package com.jk.explore.security.corsspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * A model of the browser rules that matter, speaking real HTTP to the real bank: a page may <em>send</em> some cross-origin requests without asking,
 * may <em>read</em> the answer only when the server names the page's origin (and, with credentials, also says credentials are allowed), and asks first
 * with a preflight for anything that is not simple. The preflight never carries credentials.
 */
final class Browser {

    private static final Set<String> SIMPLE_METHODS = Set.of("GET", "HEAD", "POST");
    private static final Set<String> SIMPLE_TYPES = Set.of("text/plain", "application/x-www-form-urlencoded", "multipart/form-data");

    record Reply(int status, String body, HttpResponse<String> raw) {

        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }
    }

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String bankUrl;
    private final Processed processed;

    Browser(String bankUrl, Processed processed) {
        this.bankUrl = bankUrl;
        this.processed = processed;
    }

    /**
     * A script on {@code pageOrigin} calls the bank. {@code pageOrigin} is what the browser puts in the Origin header ({@code "null"} for sandboxed frames).
     * The customer's credentials are attached when {@code withCredentials} is set.
     */
    FetchResult fetch(String pageOrigin, String method, String path, String contentType, boolean withCredentials) throws IOException, InterruptedException {
        int before = processed.list().size();
        boolean simple = SIMPLE_METHODS.contains(method) && (contentType == null || SIMPLE_TYPES.contains(contentType));
        boolean sameOrigin = pageOrigin.equals(bankUrl);
        if (!simple && !sameOrigin) {
            Reply p = preflight(pageOrigin, method, path, contentType != null);
            if (!approves(p, pageOrigin, withCredentials) || !allowed(p.header("access-control-allow-methods"), method)
                    || (contentType != null && !allowed(p.header("access-control-allow-headers"), "content-type"))) {
                return new FetchResult(processed.list().size() > before, false, null);
            }
        }
        HttpRequest.Builder actual = HttpRequest.newBuilder(URI.create(bankUrl + path)).header("Origin", pageOrigin);
        if (contentType != null) {
            actual.header("Content-Type", contentType);
        }
        if (withCredentials) {
            actual.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("ana:" + BankSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }
        HttpRequest.BodyPublisher body = method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("{}");
        Reply r = send(actual.method(method, body).build());
        boolean reached = processed.list().size() > before;
        boolean readable = sameOrigin || (reached && approves(r, pageOrigin, withCredentials));
        return new FetchResult(reached, readable, readable ? r.body() : null);
    }

    /** The question a browser asks first. It never carries the customer's credentials. */
    Reply preflight(String pageOrigin, String method, String path, boolean contentTypeHeader) throws IOException, InterruptedException {
        HttpRequest.Builder pre = HttpRequest.newBuilder(URI.create(bankUrl + path)).method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", pageOrigin).header("Access-Control-Request-Method", method);
        if (contentTypeHeader) {
            pre.header("Access-Control-Request-Headers", "content-type");
        }
        return send(pre.build());
    }

    /** The reply to a request that any script could send, with the headers kept so the demo can print them. */
    Reply raw(String method, String path, String origin, boolean withCredentials) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(bankUrl + path));
        if (origin != null) {
            b.header("Origin", origin);
        }
        if (withCredentials) {
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("ana:" + BankSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }
        return send(b.method(method, method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("{}")).build());
    }

    /** The browser's check on a response: Access-Control-Allow-Origin must name the page (or be {@code *} without credentials). */
    static boolean approves(Reply r, String pageOrigin, boolean withCredentials) {
        String allow = r.header("access-control-allow-origin");
        if (allow == null || r.status() >= 400) {
            return false;
        }
        if (withCredentials) {
            return allow.equals(pageOrigin) && "true".equals(r.header("access-control-allow-credentials"));   // "*" is refused when credentials are involved
        }
        return allow.equals("*") || allow.equals(pageOrigin);
    }

    private Reply send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body(), r);
    }

    private static boolean allowed(String list, String item) {
        if (list == null) {
            return false;
        }
        for (String s : list.split(",")) {
            if (s.trim().equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
}
