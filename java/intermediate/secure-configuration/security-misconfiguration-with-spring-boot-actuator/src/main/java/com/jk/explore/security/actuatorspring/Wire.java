package com.jk.explore.security.actuatorspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A plain HTTP client for the loopback servers. Credentials, when given, go in a Basic header. */
final class Wire {

    record Reply(int status, String body) {
    }

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    Reply get(String url, String user) throws IOException, InterruptedException {
        return send(builder(url, user).GET().build());
    }

    Reply post(String url, String user, String json) throws IOException, InterruptedException {
        return send(builder(url, user).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    /** Streams a reply and reports whether it contains the bytes, without holding a heap dump in memory. */
    int status(String url, String user) throws IOException, InterruptedException {
        return client.send(builder(url, user).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    boolean downloadContains(String url, String user, String needle) throws IOException, InterruptedException {
        HttpResponse<java.io.InputStream> r = client.send(builder(url, user).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) {
            r.body().close();
            return false;
        }
        byte[] pattern = needle.getBytes(StandardCharsets.ISO_8859_1);
        byte[] buf = new byte[1 << 16];
        int keep = pattern.length - 1;
        int filled = 0;
        try (java.io.InputStream in = r.body()) {
            int n;
            while ((n = in.read(buf, filled, buf.length - filled)) > 0) {
                int end = filled + n;
                if (indexOf(buf, end, pattern) >= 0) {
                    return true;
                }
                System.arraycopy(buf, Math.max(0, end - keep), buf, 0, Math.min(keep, end));
                filled = Math.min(keep, end);
            }
        }
        return false;
    }

    private static int indexOf(byte[] data, int end, byte[] pattern) {
        outer:
        for (int i = 0; i + pattern.length <= end; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private HttpRequest.Builder builder(String url, String user) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url));
        if (user != null) {
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + StoreSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }
        return b;
    }

    private Reply send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body());
    }
}
