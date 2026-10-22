package com.jk.explore.security.uploadspring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** A tiny HTTP client for the demo: a plain GET, and a multipart upload with the file name exactly as given. Redirects are not followed. */
final class Wire {

    record Reply(int status, String body, String contentType, String disposition, boolean noSniff, String location) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private Wire() {
    }

    static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    static Reply get(int port, String pathAndQuery) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + pathAndQuery)).GET().build());
    }

    static Reply upload(int port, String fileName, byte[] content) throws IOException, InterruptedException {
        String boundary = "lab-boundary-1f2e3d";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        body.writeBytes(content);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1));
        return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/uploads"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build());
    }

    private static Reply send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        var headers = response.headers();
        return new Reply(response.statusCode(), response.body(), headers.firstValue("Content-Type").orElse(""),
                headers.firstValue("Content-Disposition").orElse(""), headers.firstValue("X-Content-Type-Options").orElse("").equals("nosniff"),
                headers.firstValue("Location").orElse(""));
    }
}
