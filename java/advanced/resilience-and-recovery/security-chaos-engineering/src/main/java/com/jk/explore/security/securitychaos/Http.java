package com.jk.explore.security.securitychaos;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** A client with a hard deadline for the whole call, body included. A trickle of bytes cannot keep it waiting. */
final class Http implements AutoCloseable {

    private final HttpClient client;
    private final long deadlineMillis;

    Http(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(deadlineMillis)).build();
    }

    /** The first line of a 200 answer, or empty on any failure: refused, reset, slow, cut short or not 200. */
    Optional<String> get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET().build());
    }

    Optional<String> post(String url, String body) {
        return send(HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private Optional<String> send(HttpRequest request) {
        CompletableFuture<HttpResponse<String>> call = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> response = call.get(deadlineMillis, TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return response.body().lines().findFirst();
        } catch (TimeoutException | ExecutionException e) {
            call.cancel(true);
            return Optional.empty();
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
