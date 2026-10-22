package com.jk.explore.security.ssrfspring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * One request over Spring's {@link RestClient}, to an address that has already been chosen, with the original host in the Host header.
 * It never follows a redirect (the importer decides), reads at most a fixed number of bytes and gives up after a timeout.
 */
final class Fetcher {

    static final int MAX_BYTES = 1024;
    static final Duration TIMEOUT = Duration.ofMillis(1500);

    record Reply(int status, String contentType, String location, String body, boolean tooLarge) {
    }

    /** Raised when nothing answered in time or the connection failed. */
    static final class NoAnswer extends Exception {

        NoAnswer(String message) {
            super(message);
        }
    }

    private final RestClient client;

    Fetcher() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(TIMEOUT).build());
        factory.setReadTimeout(TIMEOUT);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    Reply get(java.net.InetSocketAddress where, String hostHeader, String path) throws NoAnswer {
        String literal = where.getAddress() instanceof java.net.Inet6Address ? "[" + where.getAddress().getHostAddress() + "]" : where.getAddress().getHostAddress();
        URI uri = URI.create("http://" + literal + ":" + where.getPort() + path);
        try {
            return client.get().uri(uri).header(HttpHeaders.HOST, hostHeader).exchange((request, response) -> {
                try (InputStream in = response.getBody()) {
                    byte[] bytes = in.readNBytes(MAX_BYTES + 1);
                    boolean tooLarge = bytes.length > MAX_BYTES;
                    String type = response.getHeaders().getContentType() == null ? "" : response.getHeaders().getContentType().toString();
                    return new Reply(response.getStatusCode().value(), type, response.getHeaders().getFirst(HttpHeaders.LOCATION),
                            new String(bytes, 0, Math.min(bytes.length, MAX_BYTES), StandardCharsets.UTF_8), tooLarge);
                }
            });
        } catch (ResourceAccessException e) {
            throw new NoAnswer("no answer within " + TIMEOUT.toMillis() + " ms");
        }
    }
}
