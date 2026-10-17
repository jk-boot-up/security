package com.jk.explore.security.httpsnginx;

import java.util.List;

/** Writes the nginx configuration for each version of the online store's front door. */
public final class NginxConfig {

    /** The five security headers the auditor looks for. */
    public static final List<String[]> HEADERS = List.of(
            new String[] {"Strict-Transport-Security", "max-age=63072000; includeSubDomains"},
            new String[] {"Content-Security-Policy", "default-src 'self'"},
            new String[] {"X-Content-Type-Options", "nosniff"},
            new String[] {"X-Frame-Options", "DENY"},
            new String[] {"Referrer-Policy", "no-referrer"});

    public enum Variant {
        /** TLS 1.3 only, HSTS, a redirect from the plain port, and a proxy to the application. */
        CAREFUL,
        /** The same, but TLS 1.2 is allowed too, with a short list of strong ciphers. */
        COMPAT,
        /** The careful version, but the server presents a certificate nobody trusts. */
        IMPOSTOR,
        /** The careful version, and clients must present a certificate signed by the demo root. */
        MTLS,
        /** The plain port also proxies to the application, which is told whatever protocol the client claims. */
        PROXY_TRAP,
        /** The plain port also proxies to the application, which is told the protocol nginx saw. */
        PROXY_SAFE
    }

    private NginxConfig() {
    }

    public static String text(Variant variant) {
        return text(variant, "shop");
    }

    /** The configuration, naming which certificate and key under /pki the server uses ("shop" or "missing" for a deliberately broken one). */
    static String text(Variant variant, String certificate) {
        String cert = variant == Variant.IMPOSTOR ? "impostor" : certificate;
        boolean compat = variant == Variant.COMPAT;
        boolean mtls = variant == Variant.MTLS;
        boolean plainProxy = variant == Variant.PROXY_TRAP || variant == Variant.PROXY_SAFE;
        StringBuilder c = new StringBuilder();
        c.append("events {}\n");
        c.append("http {\n");
        c.append("  server_tokens off;\n");
        c.append("  access_log off;\n");
        c.append("\n");
        c.append("  # the application: reachable only inside the container, and it says what it was told\n");
        c.append("  server {\n");
        c.append("    listen 127.0.0.1:8081;\n");
        c.append("    location / {\n");
        c.append("      return 200 \"app saw proto=$http_x_forwarded_proto client=$http_x_client_dn\\n\";\n");
        c.append("    }\n");
        c.append("  }\n");
        c.append("\n");
        c.append("  # the plain port: only a redirect to https\n");
        c.append("  server {\n");
        c.append("    listen 80;\n");
        c.append("    server_name shop.example.test;\n");
        c.append("    location = /health {\n");
        c.append("      return 200 \"ok\\n\";\n");
        c.append("    }\n");
        if (plainProxy) {
            String proto = variant == Variant.PROXY_TRAP ? "$http_x_forwarded_proto" : "$scheme";
            c.append("    location /app {\n");
            c.append("      proxy_pass http://127.0.0.1:8081/;\n");
            c.append("      proxy_set_header X-Forwarded-Proto ").append(proto).append(";\n");
            c.append("    }\n");
        }
        c.append("    location / {\n");
        c.append("      return 301 https://$host$request_uri;\n");
        c.append("    }\n");
        c.append("  }\n");
        c.append("\n");
        c.append("  server {\n");
        c.append("    listen 443 ssl;\n");
        c.append("    http2 on;\n");
        c.append("    server_name shop.example.test;\n");
        c.append("\n");
        c.append("    ssl_certificate /pki/").append(cert).append(".crt;\n");
        c.append("    ssl_certificate_key /pki/").append(cert).append(".key;\n");
        if (compat) {
            c.append("    ssl_protocols TLSv1.2 TLSv1.3;\n");
            c.append("    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305;\n");
            c.append("    ssl_prefer_server_ciphers off;\n");
        } else {
            c.append("    ssl_protocols TLSv1.3;\n");
        }
        c.append("    ssl_session_tickets off;\n");
        if (mtls) {
            c.append("    ssl_client_certificate /pki/root.pem;\n");
            c.append("    ssl_verify_client on;\n");
        }
        c.append("\n");
        for (String[] header : HEADERS) {
            c.append("    add_header ").append(header[0]).append(" \"").append(header[1]).append("\" always;\n");
        }
        c.append("\n");
        c.append("    location / {\n");
        c.append("      proxy_pass http://127.0.0.1:8081;\n");
        c.append("      proxy_set_header X-Forwarded-Proto $scheme;\n");
        c.append("      proxy_set_header X-Client-DN $ssl_client_s_dn;\n");
        c.append("    }\n");
        c.append("  }\n");
        c.append("}\n");
        return c.toString();
    }
}
