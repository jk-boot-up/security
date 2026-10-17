package com.jk.explore.security.httpnginx;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Sends raw bytes to a port and reads the whole answer, so malformed requests can be sent that an HTTP client library would never send. */
public final class Probe {

    /** An HTTP reply. Header names are lower case. */
    public record Reply(int status, Map<String, String> headers, String body) {
    }

    private Probe() {
    }

    public static Reply get(int port, String path) throws IOException {
        return send(port, "GET " + path + " HTTP/1.1\r\nHost: shop.example\r\nConnection: close\r\n\r\n");
    }

    public static Reply send(int port, String raw) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(raw.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().flush();
            return parse(socket.getInputStream().readAllBytes());
        }
    }

    static Reply parse(byte[] bytes) throws IOException {
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        int split = text.indexOf("\r\n\r\n");
        if (split < 0) {
            throw new IOException("no complete reply");
        }
        String[] lines = text.substring(0, split).split("\r\n");
        String[] status = lines[0].split(" ", 3);
        if (status.length < 2 || !status[0].startsWith("HTTP/")) {
            throw new IOException("not an HTTP reply: " + lines[0]);
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                String name = lines[i].substring(0, colon).toLowerCase(java.util.Locale.ROOT);
                String value = lines[i].substring(colon + 1).strip();
                headers.merge(name, value, (a, b) -> a + ", " + b);
            }
        }
        return new Reply(Integer.parseInt(status[1]), headers, text.substring(split + 4));
    }
}
