package com.jk.explore.security.tlsservers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** A real Caddy on loopback ports, with its own local certificate authority, run in a folder of its own. */
final class Caddy implements AutoCloseable {

    private final Path dir;
    private final int httpPort;
    private final int httpsPort;
    private Process process;

    Caddy(Path dir) throws IOException {
        this.dir = Files.createDirectories(dir);
        this.httpPort = Procs.freePort();
        this.httpsPort = Procs.freePort();
    }

    int httpPort() {
        return httpPort;
    }

    int httpsPort() {
        return httpsPort;
    }

    /** Where Caddy keeps the root of its local authority once it has made one. */
    Path root() {
        return dir.resolve("data/caddy/pki/authorities/local/root.crt");
    }

    String caddyfile(boolean hsts) {
        return """
                {
                    admin off
                    local_certs
                    skip_install_trust
                    http_port %d
                    https_port %d
                }
                www.shop.localhost {
                    bind 127.0.0.1
                    %s
                    respond "the shop"
                }
                """.formatted(httpPort, httpsPort, hsts ? "header Strict-Transport-Security \"max-age=31536000; includeSubDomains\"" : "");
    }

    void start(boolean hsts) throws IOException, InterruptedException {
        close();
        Files.writeString(dir.resolve("Caddyfile"), caddyfile(hsts));
        ProcessBuilder pb = new ProcessBuilder("caddy", "run", "--config", dir.resolve("Caddyfile").toString(), "--adapter", "caddyfile").redirectErrorStream(true)
                .redirectOutput(dir.resolve("caddy.log").toFile());
        pb.environment().put("XDG_DATA_HOME", dir.resolve("data").toString());
        pb.environment().put("XDG_CONFIG_HOME", dir.resolve("config").toString());
        process = pb.start();
        for (int i = 0; i < 200; i++) {
            if (Files.exists(root())) {
                try {
                    Https.Response r = Https.get(httpsPort, "www.shop.localhost", "/", Https.trusting(root()));
                    if (!r.refused()) {
                        return;
                    }
                } catch (IOException | java.security.GeneralSecurityException e) {
                    // not ready yet
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("caddy did not start: " + Files.readString(dir.resolve("caddy.log")));
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
