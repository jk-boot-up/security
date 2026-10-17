package com.jk.explore.security.https;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

/**
 * A client that verifies the server: it trusts only the shop's certificate and uses the standard HTTPS host name check
 * (java.net.http does it by default). It never follows redirects by itself, so the demo can look at them, and it keeps cookies the way a browser would.
 */
final class ShopClient {

    private final HttpClient client;
    private final CookieManager cookies = new CookieManager();

    ShopClient(SSLContext trust, String... protocols) {
        SSLParameters parameters = new SSLParameters();
        if (protocols.length > 0) {
            parameters.setProtocols(protocols);
        }
        client = HttpClient.newBuilder()
                .sslContext(trust)
                .sslParameters(parameters)
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    HttpResponse<String> get(String url) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Whether this client would attach its stored cookies to a request for the address. */
    boolean wouldSendCookie(String url) throws IOException {
        return !cookies.get(URI.create(url), java.util.Map.of()).getOrDefault("Cookie", java.util.List.of()).isEmpty();
    }

    /** The first TLS error behind an exception, as its class name; or "none". */
    static String tlsFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SSLException) {
                return t.getClass().getSimpleName();
            }
        }
        return error.getClass().getSimpleName();
    }

    /** "ok (protocol)" if the request succeeds, otherwise "refused (error class)". */
    String attempt(String url) throws InterruptedException {
        try {
            HttpResponse<String> response = get(url);
            return "ok (" + response.sslSession().map(s -> s.getProtocol()).orElse("no TLS") + ")";
        } catch (IOException e) {
            return "refused (" + tlsFailure(e) + ")";
        }
    }
}
