package com.jk.explore.security.httpsnginx;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads an nginx configuration as text and scores it against seven TLS habits, one point each. It starts nothing, so it needs no Docker. */
public final class ConfigScore {

    public record Finding(String check, boolean passed, String reason) {
    }

    private static final Pattern PROTOCOLS = Pattern.compile("ssl_protocols\\s+([^;]+);");
    private static final java.util.Set<String> OLD_PROTOCOLS = java.util.Set.of("SSLv2", "SSLv3", "TLSv1", "TLSv1.1");

    private ConfigScore() {
    }

    public static List<Finding> score(String conf) {
        List<Finding> findings = new ArrayList<>();
        var protocols = PROTOCOLS.matcher(conf);
        if (!protocols.find()) {
            findings.add(new Finding("modern protocols only", false, "ssl_protocols is not set, so nginx's defaults decide"));
        } else if (java.util.Arrays.stream(protocols.group(1).strip().split("\\s+")).anyMatch(OLD_PROTOCOLS::contains)) {
            findings.add(new Finding("modern protocols only", false, "allows TLS 1.1 or older"));
        } else {
            findings.add(new Finding("modern protocols only", true, protocols.group(1).strip()));
        }
        boolean hsts = conf.contains("Strict-Transport-Security");
        boolean hstsAlways = Pattern.compile("add_header\\s+Strict-Transport-Security\\s+\"[^\"]*\"\\s+always;").matcher(conf).find();
        findings.add(new Finding("HSTS on every answer", hstsAlways, !hsts ? "missing" : hstsAlways ? "with always" : "without always, error pages lose it"));
        boolean redirect = Pattern.compile("return\\s+301\\s+https://").matcher(conf).find();
        findings.add(new Finding("plain port redirects", redirect, redirect ? "301 to https" : "no 301 to https"));
        boolean tickets = Pattern.compile("ssl_session_tickets\\s+off;").matcher(conf).find();
        findings.add(new Finding("session tickets off", tickets, tickets ? "off" : "on by default"));
        boolean tokens = Pattern.compile("server_tokens\\s+off;").matcher(conf).find();
        findings.add(new Finding("version hidden", tokens, tokens ? "server_tokens off" : "the version is shown"));
        boolean http2 = Pattern.compile("http2\\s+on;").matcher(conf).find();
        findings.add(new Finding("HTTP/2", http2, http2 ? "on" : "off"));
        boolean passThrough = conf.contains("$http_x_forwarded_proto") && Pattern.compile("proxy_set_header\\s+X-Forwarded-Proto\\s+\\$http_x_forwarded_proto").matcher(conf).find();
        boolean sets = Pattern.compile("proxy_set_header\\s+X-Forwarded-Proto\\s+\\$scheme;").matcher(conf).find();
        boolean ok = sets && !passThrough;
        findings.add(new Finding("app told what nginx saw", ok, passThrough ? "copies the client's X-Forwarded-Proto" : sets ? "X-Forwarded-Proto is $scheme" : "never set"));
        return findings;
    }

    public static int points(List<Finding> findings) {
        return (int) findings.stream().filter(Finding::passed).count();
    }
}
