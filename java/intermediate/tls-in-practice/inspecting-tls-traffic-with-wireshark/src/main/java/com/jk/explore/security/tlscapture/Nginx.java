package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** A real nginx with a login page, on a loopback port. */
final class Nginx implements AutoCloseable {

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

    String config(String protocols, String certificate, String key) {
        return """
                worker_processes 1;
                daemon off;
                pid %1$s/nginx.pid;
                error_log %1$s/error.log warn;
                events { worker_connections 64; }
                http {
                    access_log off;
                    client_body_temp_path %1$s/tmp-body;
                    proxy_temp_path %1$s/tmp-proxy;
                    fastcgi_temp_path %1$s/tmp-fastcgi;
                    uwsgi_temp_path %1$s/tmp-uwsgi;
                    scgi_temp_path %1$s/tmp-scgi;
                    server {
                        listen 127.0.0.1:%2$d ssl;
                        server_name www.shop.localhost;
                        ssl_certificate %3$s;
                        ssl_certificate_key %4$s;
                        ssl_protocols %5$s;
                        location /login {
                            add_header Set-Cookie "session=7f3a; Secure; HttpOnly" always;
                            return 200 "welcome ana";
                        }
                        location / { return 200 "the shop"; }
                    }
                }
                """.formatted(dir, port, certificate, key, protocols);
    }

    void start(String protocols, String certificate, String key) throws IOException, InterruptedException {
        Files.writeString(dir.resolve("nginx.conf"), config(protocols, certificate, key));
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
