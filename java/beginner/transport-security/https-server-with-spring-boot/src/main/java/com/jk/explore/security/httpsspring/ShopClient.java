package com.jk.explore.security.httpsspring;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * A client built from an SSL bundle's context. It verifies the server's certificate and its host name (java.net.http checks both by default),
 * never follows redirects by itself so the demo can look at them, and keeps cookies the way a browser would.
 */
final class ShopClient {

    private final HttpClient client;
    private final CookieManager cookies = new CookieManager();

    /** The context from an SSL bundle, or null for the JDK's own default trust. Protocols limit what the client offers. */
    ShopClient(SSLContext context, String... protocols) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10));
        if (context != null) {
            builder.sslContext(context);
        }
        if (protocols.length > 0) {
            SSLParameters parameters = new SSLParameters();
            parameters.setProtocols(protocols);
            builder.sslParameters(parameters);
        }
        client = builder.build();
    }

    Reply get(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        return Reply.of(response);
    }

    /** The same request made through Spring's RestClient, which sits on the same HttpClient and so on the same trust. */
    String restGet(String url) {
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(client)).build().get().uri(url).retrieve().body(String.class);
    }

    /** Whether this client would attach its stored cookies to a request for the address. */
    boolean wouldSendCookie(String url) throws IOException {
        return !cookies.get(URI.create(url), java.util.Map.of()).getOrDefault("Cookie", java.util.List.of()).isEmpty();
    }

    /** "ok" with the status, or "refused" when the connection could not be made safely. */
    String attempt(String url) throws InterruptedException {
        try {
            return "ok (" + get(url).status() + ")";
        } catch (IOException e) {
            return "refused";
        }
    }
}
