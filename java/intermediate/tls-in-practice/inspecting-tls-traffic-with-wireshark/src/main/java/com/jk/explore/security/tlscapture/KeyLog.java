package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** A key log file (the format SSLKEYLOGFILE writes): one line per secret, each a label, the connection's client random and the secret, in hex. */
record KeyLog(List<Line> lines) {

    record Line(String label, String clientRandom, String secret) {
    }

    /** What each TLS 1.3 secret opens, in the order the handshake makes them. */
    static final Map<String, String> OPENS = new java.util.LinkedHashMap<>();

    static {
        OPENS.put("CLIENT_HANDSHAKE_TRAFFIC_SECRET", "the client's handshake");
        OPENS.put("SERVER_HANDSHAKE_TRAFFIC_SECRET", "the server's handshake");
        OPENS.put("CLIENT_TRAFFIC_SECRET_0", "the client's data");
        OPENS.put("SERVER_TRAFFIC_SECRET_0", "the server's data");
        OPENS.put("EXPORTER_SECRET", "no traffic");
    }

    static KeyLog parse(List<String> text) {
        return new KeyLog(text.stream().filter(l -> !l.isBlank() && !l.startsWith("#")).map(l -> l.split(" ")).map(p -> new Line(p[0], p[1], p[2])).toList());
    }

    static KeyLog read(Path file) throws IOException {
        return parse(Files.readAllLines(file));
    }

    /** True when a line of this log is for the connection whose Client Hello carried {@code clientRandom}: that is the only way a log names a connection. */
    boolean opens(String clientRandom) {
        return lines.stream()
                .anyMatch(l -> l.clientRandom().equalsIgnoreCase(clientRandom));
    }

    Line line(String label) {
        return lines.stream().filter(l -> l.label().equals(label)).findFirst().orElseThrow();
    }

    /** A log with only the lines whose label starts with {@code prefix}. */
    KeyLog only(String prefix) {
        return new KeyLog(lines.stream().filter(l -> l.label().startsWith(prefix)).toList());
    }

    KeyLog plus(KeyLog other) {
        return new KeyLog(java.util.stream.Stream.concat(lines.stream(), other.lines.stream()).toList());
    }

    String text() {
        StringBuilder out = new StringBuilder();
        lines.forEach(l -> out.append(l.label()).append(' ').append(l.clientRandom()).append(' ').append(l.secret()).append('\n'));
        return out.toString();
    }

}
