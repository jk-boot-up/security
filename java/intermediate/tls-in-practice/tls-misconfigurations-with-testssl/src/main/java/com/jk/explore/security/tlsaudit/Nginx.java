package com.jk.explore.security.tlsaudit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** A real nginx on a loopback port, with a TLS configuration written for each case. */
final class Nginx implements AutoCloseable {

    /** The choices that the audit looks at. */
    record Settings(String protocols, boolean serverTokens, boolean hsts, boolean securityHeaders, boolean modernCiphers) {

        /** What a configuration copied from the first search result tends to look like. */
        static Settings copied() {
            return new Settings("TLSv1.2 TLSv1.3", true, false, false, false);
        }

        /** What the guidance asks for. */
        static Settings guided() {
            return new Settings("TLSv1.2 TLSv1.3", false, true, true, true);
        }
    }

    private final Path dir;
    private final int port;
    private Process process;

    Nginx(Path dir) throws IOException {
        this.dir = Files.createDirectories(dir);
        this.port = Procs.freePort();
    }

    int port() {
        return port;
    }

    String config(Settings s, String certificate, String key) {
        return """
                worker_processes 1;
                daemon off;
                pid %1$s/nginx.pid;
                error_log %1$s/error.log warn;
                events { worker_connections 64; }
                http {
                    access_log off;
                    server_tokens %2$s;
                    client_body_temp_path %1$s/tmp-body;
                    proxy_temp_path %1$s/tmp-proxy;
                    fastcgi_temp_path %1$s/tmp-fastcgi;
                    uwsgi_temp_path %1$s/tmp-uwsgi;
                    scgi_temp_path %1$s/tmp-scgi;
                    server {
                        listen 127.0.0.1:%3$d ssl;
                        server_name www.shop.localhost;
                        ssl_certificate %4$s;
                        ssl_certificate_key %5$s;
                        ssl_protocols %6$s;
                        %7$s
                        %8$s
                        %9$s
                        location / { return 200 "the shop"; }
                    }
                }
                """.formatted(dir, s.serverTokens() ? "on" : "off", port, certificate, key, s.protocols(),
                s.hsts() ? "add_header Strict-Transport-Security \"max-age=63072000; includeSubDomains\" always;" : "",
                s.modernCiphers() ? "ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;" : "",
                s.securityHeaders() ? "add_header X-Content-Type-Options nosniff always; add_header X-Frame-Options DENY always; add_header Content-Security-Policy \"default-src 'self'\" always; add_header Referrer-Policy no-referrer always;" : "");
    }

    void start(Settings settings, String certificate, String key) throws IOException, InterruptedException {
        Files.writeString(dir.resolve("nginx.conf"), config(settings, certificate, key));
        process = new ProcessBuilder("nginx", "-p", dir.toString(), "-c", dir.resolve("nginx.conf").toString(), "-e", dir.resolve("error.log").toString()).redirectErrorStream(true)
                .redirectOutput(dir.resolve("stdout.log").toFile()).start();
        for (int i = 0; i < 100; i++) {
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("nginx did not start: " + Files.readString(dir.resolve("stdout.log")));
    }

    @Override
    public void close() throws InterruptedException {
        if (process != null) {
            process.destroy();
            process.waitFor(10, TimeUnit.SECONDS);
            process = null;
        }
    }
}
