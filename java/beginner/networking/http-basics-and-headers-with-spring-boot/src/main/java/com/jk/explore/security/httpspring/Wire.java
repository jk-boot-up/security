package com.jk.explore.security.httpspring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Talks to a running store over the loopback interface: with the JDK's HTTP client, and with raw bytes for requests a client would never send. */
final class Wire {

    record Reply(int status, Map<String, List<String>> headers) {

        /** The first value of each header, with names as the server wrote them. */
        Map<String, String> firstValues() {
            Map<String, String> first = new LinkedHashMap<>();
            headers.forEach((name, values) -> first.put(name, values.get(0)));
            return first;
        }

        List<String> all(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return List.of();
        }
    }

    private Wire() {
    }

    static Reply get(int port, String path, Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        extraHeaders.forEach(request::header);
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, List<String>> headers = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> headers.put(name, new ArrayList<>(values)));
        return new Reply(response.statusCode(), headers);
    }

    /** Sends the bytes as they are and returns the status code of the answer. */
    static int rawStatus(int port, String rawRequest) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(10_000);
            socket.getOutputStream().write(rawRequest.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().flush();
            String statusLine = new String(socket.getInputStream().readNBytes(12), StandardCharsets.ISO_8859_1);
            return Integer.parseInt(statusLine.substring(9, 12));
        }
    }
}
