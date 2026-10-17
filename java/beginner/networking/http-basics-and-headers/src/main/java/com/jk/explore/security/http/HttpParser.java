package com.jk.explore.security.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A small, strict HTTP/1.1 request parser. Strict is the point: when two programs in a chain (a proxy and a server) read the
 * same bytes differently, an attacker can hide a second request inside the first ("request smuggling"). So anything ambiguous is refused.
 */
final class HttpParser {

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH");

    private HttpParser() {
    }

    static HttpRequest parse(String raw) throws BadMessageException {
        int split = raw.indexOf("\r\n\r\n");
        if (split < 0) {
            throw new BadMessageException("no blank line after the headers");
        }
        String head = raw.substring(0, split);
        String bodyText = raw.substring(split + 4);
        if (head.replace("\r\n", "").contains("\n") || head.replace("\r\n", "").contains("\r")) {
            throw new BadMessageException("a bare line feed or carriage return in the headers");
        }
        String[] lines = head.split("\r\n", -1);
        String[] start = lines[0].split(" ", -1);
        if (start.length != 3 || !METHODS.contains(start[0]) || !start[1].startsWith("/") || !start[2].equals("HTTP/1.1")) {
            throw new BadMessageException("bad request line");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(" ") || line.startsWith("\t")) {
                throw new BadMessageException("a folded header line");
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new BadMessageException("a header without a name");
            }
            String name = line.substring(0, colon);
            if (!name.matches("[A-Za-z0-9-]+")) {
                throw new BadMessageException("a header name with a space or odd character: '" + name + "'");
            }
            String value = line.substring(colon + 1).strip();
            String key = name.toLowerCase();
            if (headers.containsKey(key)) {
                if (key.equals("content-length") && !headers.get(key).equals(value)) {
                    throw new BadMessageException("two different Content-Length headers");
                }
                if (!key.equals("content-length")) {
                    value = headers.get(key) + ", " + value;
                }
            }
            headers.put(key, value);
        }
        if (!headers.containsKey("host")) {
            throw new BadMessageException("no Host header");
        }
        if (headers.containsKey("transfer-encoding") && headers.containsKey("content-length")) {
            throw new BadMessageException("both Content-Length and Transfer-Encoding");
        }
        if (headers.containsKey("transfer-encoding")) {
            throw new BadMessageException("Transfer-Encoding is not supported");
        }
        byte[] body = bodyText.getBytes(StandardCharsets.ISO_8859_1);
        String length = headers.get("content-length");
        if (length != null) {
            if (!length.matches("[0-9]{1,9}")) {
                throw new BadMessageException("Content-Length is not a plain number");
            }
            int expected = Integer.parseInt(length);
            if (body.length != expected) {
                throw new BadMessageException("Content-Length says " + expected + " bytes but " + body.length + " follow");
            }
        } else if (body.length != 0) {
            throw new BadMessageException("a body with no Content-Length");
        }
        return new HttpRequest(start[0], start[1], start[2], headers, body);
    }
}
