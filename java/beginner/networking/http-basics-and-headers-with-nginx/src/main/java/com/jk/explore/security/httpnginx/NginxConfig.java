package com.jk.explore.security.httpnginx;

import java.util.List;

/** Writes the nginx configuration for each version of the online store's front door. */
public final class NginxConfig {

    /** The five security headers the auditor looks for. */
    public static final List<String[]> HEADERS = List.of(
            new String[] {"Strict-Transport-Security", "max-age=31536000; includeSubDomains"},
            new String[] {"Content-Security-Policy", "default-src 'self'"},
            new String[] {"X-Content-Type-Options", "nosniff"},
            new String[] {"X-Frame-Options", "DENY"},
            new String[] {"Referrer-Policy", "no-referrer"});

    public enum Variant {
        /** Out of the box: no headers, the version number on show. */
        BARE,
        /** Headers at server level, without "always": they vanish from error pages. */
        HEADERS_WITHOUT_ALWAYS,
        /** Headers at server level with "always", and server_tokens off. */
        HEADERS_WITH_ALWAYS,
        /** A nested location with its own add_header: it drops every server-level header. */
        INHERITANCE_TRAP,
        /** The nested location repeats the security headers. */
        INHERITANCE_FIXED,
        /** Size limits on the body and on header lines. */
        LIMITS,
        /** A proxy in front of an application that gives away its technology. */
        UPSTREAM_LEAKY,
        /** The same proxy hiding what the application gives away. */
        UPSTREAM_HIDDEN
    }

    private NginxConfig() {
    }

    private static String headers(String indent) {
        StringBuilder text = new StringBuilder();
        for (String[] header : HEADERS) {
            text.append(indent).append("add_header ").append(header[0]).append(" \"").append(header[1]).append("\" always;\n");
        }
        return text.toString();
    }

    public static String text(Variant variant) {
        StringBuilder conf = new StringBuilder();
        conf.append("events {}\n");
        conf.append("http {\n");
        boolean proxied = variant == Variant.UPSTREAM_LEAKY || variant == Variant.UPSTREAM_HIDDEN;
        if (proxied) {
            // the application: a second server inside the same nginx that gives away its technology
            conf.append("  server {\n");
            conf.append("    listen 8081;\n");
            conf.append("    server_tokens off;\n");
            conf.append("    location / {\n");
            conf.append("      add_header X-Powered-By \"PHP/5.6.40\" always;\n");
            conf.append("      add_header X-AspNet-Version \"4.0.30319\" always;\n");
            conf.append("      return 200 \"<html>shop</html>\";\n");
            conf.append("    }\n");
            conf.append("  }\n");
        }
        conf.append("  server {\n");
        conf.append("    listen 80;\n");
        if (variant != Variant.BARE && variant != Variant.UPSTREAM_LEAKY) {
            conf.append("    server_tokens off;\n");
        }
        switch (variant) {
            case HEADERS_WITHOUT_ALWAYS -> {
                for (String[] header : HEADERS) {
                    conf.append("    add_header ").append(header[0]).append(" \"").append(header[1]).append("\";\n");
                }
            }
            case HEADERS_WITH_ALWAYS, INHERITANCE_TRAP, INHERITANCE_FIXED, LIMITS -> conf.append(headers("    "));
            default -> {
            }
        }
        if (variant == Variant.LIMITS) {
            conf.append("    client_max_body_size 1k;\n");
            conf.append("    large_client_header_buffers 2 1k;\n");
        }
        conf.append("    default_type text/html;\n");
        conf.append("    location = /health { return 200 \"ok\"; }\n");
        if (proxied) {
            conf.append("    location / {\n");
            if (variant == Variant.UPSTREAM_HIDDEN) {
                conf.append("      proxy_hide_header X-Powered-By;\n");
                conf.append("      proxy_hide_header X-AspNet-Version;\n");
            }
            conf.append("      proxy_pass http://127.0.0.1:8081;\n");
            conf.append("    }\n");
        } else {
            conf.append("    location = / { return 200 \"<html>shop</html>\"; }\n");
            conf.append("    location = /missing { return 404 \"not found\"; }\n");
            conf.append("    location = /order { return 200 \"ok\"; }\n");
            if (variant == Variant.INHERITANCE_TRAP || variant == Variant.INHERITANCE_FIXED) {
                conf.append("    location = /account {\n");
                conf.append("      add_header Cache-Control \"no-store\" always;\n");
                if (variant == Variant.INHERITANCE_FIXED) {
                    conf.append(headers("      "));
                }
                conf.append("      return 200 \"<html>account</html>\";\n");
                conf.append("    }\n");
            }
        }
        conf.append("  }\n");
        conf.append("}\n");
        return conf.toString();
    }
}
