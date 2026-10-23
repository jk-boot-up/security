package com.jk.explore.security.meshmtls;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a proxy's own counters (Prometheus text) and sums one counter by the identity on the other end of the connection. */
public final class Metrics {

    /** The key used for a connection that carried no mesh identity. */
    public static final String NO_IDENTITY = "no identity";

    private static final Pattern LINE = Pattern.compile("^(\\w+)\\{([^}]*)}\\s+(\\d+)\\s*$");
    private static final Pattern LABEL = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private Metrics() {
    }

    /**
     * Sums the counter over the lines for the given direction and port, keyed by the value of idLabel
     * (client_id seen by a server, server_id seen by a client) when the connection was TLS, else by NO_IDENTITY.
     */
    public static Map<String, Long> byIdentity(String text, String counter, String direction, String port, String idLabel) {
        Map<String, Long> sums = new TreeMap<>();
        for (String line : text.split("\n")) {
            Matcher m = LINE.matcher(line.strip());
            if (!m.matches() || !m.group(1).equals(counter)) {
                continue;
            }
            Map<String, String> labels = new TreeMap<>();
            Matcher l = LABEL.matcher(m.group(2));
            while (l.find()) {
                labels.put(l.group(1), l.group(2));
            }
            if (labels.containsKey("direction") && !direction.equals(labels.get("direction"))) {
                continue;
            }
            if (!port.equals(labels.get("target_port"))) {
                continue;
            }
            String key = "true".equals(labels.get("tls")) && labels.containsKey(idLabel) ? labels.get(idLabel) : NO_IDENTITY;
            sums.merge(key, Long.parseLong(m.group(3)), Long::sum);
        }
        return sums;
    }
}
