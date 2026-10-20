package com.jk.explore.security.tlslog;

import java.util.Map;
import java.util.Optional;

/**
 * The NSS key log format that browsers, curl and Java agents write when {@code SSLKEYLOGFILE} is set and that Wireshark reads:
 * {@code LABEL <client random in hex> <secret in hex>}. One line is enough to decrypt one direction of one connection.
 */
public final class KeyLog {

    private KeyLog() {
    }

    static String lines(Capture c, Secrets s) {
        return "CLIENT_TRAFFIC_SECRET_0 " + Hex.of(c.clientRandom()) + " " + Hex.of(s.client()) + "\n"
                + "SERVER_TRAFFIC_SECRET_0 " + Hex.of(c.clientRandom()) + " " + Hex.of(s.server()) + "\n";
    }

    /** The secret for a connection and a label, or empty when the log has no line for them. */
    static Optional<byte[]> find(String log, String label, byte[] clientRandom) {
        String random = Hex.of(clientRandom);
        for (String line : log.split("\n")) {
            String[] p = line.trim().split(" ");
            if (p.length == 3 && p[0].equals(label) && p[1].equals(random)) {
                return Optional.of(Hex.parse(p[2]));
            }
        }
        return Optional.empty();
    }

    /**
     * Where a client may write a key log. The variable is honoured only outside production: a key log in a production process is a copy of every
     * session's secrets in a file, and the same as handing a recording to whoever can read the file.
     */
    public static Optional<String> pathFor(Map<String, String> environment) {
        String path = environment.get("SSLKEYLOGFILE");
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        if ("production".equalsIgnoreCase(environment.getOrDefault("APP_ENV", ""))) {
            throw new IllegalStateException("SSLKEYLOGFILE is set in production; refusing to start");
        }
        return Optional.of(path);
    }
}
