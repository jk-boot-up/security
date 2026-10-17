package com.jk.explore.security.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** A parsed HTTP/1.1 request. Header names are lower case. */
record HttpRequest(String method, String target, String version, Map<String, String> headers, byte[] body) {

    String header(String name) {
        return headers.get(name.toLowerCase());
    }

    /** Writes the request again in the wire format: a request line, headers with Content-Length, a blank line, the body. */
    String serialize() {
        StringBuilder text = new StringBuilder(method).append(' ').append(target).append(' ').append(version).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            text.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        return text.append("\r\n").append(new String(body, StandardCharsets.ISO_8859_1)).toString();
    }

    static HttpRequest of(String method, String target, String host, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", host);
        if (!body.isEmpty()) {
            headers.put("content-length", String.valueOf(body.getBytes(StandardCharsets.ISO_8859_1).length));
        }
        return new HttpRequest(method, target, "HTTP/1.1", headers, body.getBytes(StandardCharsets.ISO_8859_1));
    }
}
