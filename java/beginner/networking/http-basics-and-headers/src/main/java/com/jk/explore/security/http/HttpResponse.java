package com.jk.explore.security.http;

import java.util.LinkedHashMap;
import java.util.Map;

/** An HTTP response. Writing it refuses any header that could start a new line, which is how header injection works. */
record HttpResponse(int status, String reason, Map<String, String> headers, String body) {

    static HttpResponse ok(String body) {
        return new HttpResponse(200, "OK", new LinkedHashMap<>(), body);
    }

    HttpResponse with(String name, String value) {
        Map<String, String> copy = new LinkedHashMap<>(headers);
        copy.put(name, value);
        return new HttpResponse(status, reason, copy, body);
    }

    String serialize() throws BadMessageException {
        StringBuilder text = new StringBuilder("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (!header.getKey().matches("[A-Za-z0-9-]+")) {
                throw new BadMessageException("a header name with a space or odd character");
            }
            if (header.getValue().indexOf('\r') >= 0 || header.getValue().indexOf('\n') >= 0) {
                throw new BadMessageException("a line break inside the value of " + header.getKey());
            }
            text.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        return text.append("Content-Length: ").append(body.length()).append("\r\n\r\n").append(body).toString();
    }

    /** The redirect an online store sends after a search, with the customer's text placed in the address. */
    static HttpResponse redirectToSearch(String customerText) {
        return new HttpResponse(302, "Found", new LinkedHashMap<>(Map.of("Location", "/search?q=" + customerText)), "");
    }
}
