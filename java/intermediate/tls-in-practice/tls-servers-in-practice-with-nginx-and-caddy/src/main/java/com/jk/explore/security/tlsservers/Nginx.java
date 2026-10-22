package com.jk.explore.security.tlsservers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** A real nginx on loopback ports, with a configuration written for each case, run in a folder of its own. */
final class Nginx implements AutoCloseable {

    /** The choices that this lesson varies. {@code hsts} and {@code accountHeader} control add_header at server and location level. */
    record Settings(String protocols, boolean serverTokens, boolean hsts, boolean accountHeader, boolean hstsInAccount, boolean stapling) {

        static Settings defaults() {
            return new Settings("TLSv1.2 TLSv1.3", true, false, false, false, false);
        }

        Settings protocols(String value) {
            return new Settings(value, serverTokens, hsts, accountHeader, hstsInAccount, stapling);
        }

        Settings serverTokens(boolean value) {
            return new Settings(protocols, value, hsts, accountHeader, hstsInAccount, stapling);
        }

        Settings hsts(boolean value) {
            return new Settings(protocols, serverTokens, value, accountHeader, hstsInAccount, stapling);
        }

        Settings accountHeader(boolean value, boolean repeatHsts) {
            return new Settings(protocols, serverTokens, hsts, value, repeatHsts, stapling);
        }

        Settings stapling(boolean value) {
            return new Settings(protocols, serverTokens, hsts, accountHeader, hstsInAccount, value);
        }
    }

    static final String HSTS = "add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;";

    private final Path dir;
    private final int httpPort;
    private final int httpsPort;
    private final String certificate;
    private final String key;
    private final String root;
    private Process process;

    Nginx(Path dir, String certificate, String key, String root) throws IOException {
        this.root = root;
        this.dir = Files.createDirectories(dir);
        this.httpPort = Procs.freePort();
        this.httpsPort = Procs.freePort();
        this.certificate = certificate;
        this.key = key;
    }

    int httpPort() {
        return httpPort;
    }

    int httpsPort() {
        return httpsPort;
    }

    String errorLog() throws IOException {
        return Files.readString(dir.resolve("error.log"));
    }

    String config(Settings s) {
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
                        listen 127.0.0.1:%3$d;
                        server_name www.shop.localhost;
                        return 301 https://www.shop.localhost:%4$d$request_uri;
                    }
                    server {
                        listen 127.0.0.1:%4$d ssl;
                        server_name www.shop.localhost;
                        ssl_certificate %5$s;
                        ssl_certificate_key %6$s;
                        ssl_protocols %7$s;
                        %8$s
                        %9$s
                        location / { return 200 "the shop"; }
                        location /account {
                            %10$s
                            %11$s
                            return 200 "the account page";
                        }
                    }
                }
                """.formatted(dir, s.serverTokens() ? "on" : "off", httpPort, httpsPort, certificate, key, s.protocols(),
                s.hsts() ? HSTS : "", s.stapling() ? "ssl_stapling on; ssl_trusted_certificate " + root + ";" : "",
                s.accountHeader() ? "add_header X-Frame-Options DENY always;" : "", s.hstsInAccount() ? HSTS : "");
    }

    /** Writes the configuration and starts nginx, or reloads it if it is already running. */
    void start(Settings settings) throws IOException, InterruptedException {
        Files.writeString(dir.resolve("nginx.conf"), config(settings));
        if (process != null && process.isAlive()) {
            Procs.run(java.util.List.of("nginx", "-p", dir.toString(), "-c", dir.resolve("nginx.conf").toString(), "-e", dir.resolve("error.log").toString(), "-s", "reload"));
            Thread.sleep(500);
            return;
        }
        Files.deleteIfExists(dir.resolve("error.log"));
        process = new ProcessBuilder("nginx", "-p", dir.toString(), "-c", dir.resolve("nginx.conf").toString(), "-e", dir.resolve("error.log").toString()).redirectErrorStream(true)
                .redirectOutput(dir.resolve("stdout.log").toFile()).start();
        for (int i = 0; i < 100; i++) {
            try (java.net.Socket s = new java.net.Socket(java.net.InetAddress.getLoopbackAddress(), httpsPort)) {
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("nginx did not start: " + Files.readString(dir.resolve("stdout.log")));
    }

    /** Stops nginx and starts it again with other settings, so that a change of protocols or headers is certain to be in effect. */
    void restart(Settings settings) throws IOException, InterruptedException {
        stop();
        start(settings);
    }

    void reload() throws IOException, InterruptedException {
        Procs.run(java.util.List.of("nginx", "-p", dir.toString(), "-c", dir.resolve("nginx.conf").toString(), "-e", dir.resolve("error.log").toString(), "-s", "reload"));
        Thread.sleep(500);
    }

    private void stop() throws InterruptedException {
        if (process != null) {
            process.destroy();
            process.waitFor(10, TimeUnit.SECONDS);
            process = null;
        }
    }

    @Override
    public void close() throws InterruptedException {
        stop();
    }
}
