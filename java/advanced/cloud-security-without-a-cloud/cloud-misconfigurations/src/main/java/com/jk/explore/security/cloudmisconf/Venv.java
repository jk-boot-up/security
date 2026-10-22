package com.jk.explore.security.cloudmisconf;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A Python virtual environment under build/ with Moto (an AWS emulator) and Checkov (an infrastructure-as-code scanner) installed with pip the first time. */
public final class Venv {

    static final String MOTO = "5.2.4";
    static final String CHECKOV = "3.3.30";

    private final Path dir;

    public Venv(Path build) throws IOException, InterruptedException {
        this.dir = build.toAbsolutePath().resolve("cloud-venv");
        Path marker = dir.resolve("installed-" + MOTO + "-" + CHECKOV);
        if (!Files.exists(marker)) {
            run(List.of("python3", "-m", "venv", dir.toString()));
            run(List.of(dir.resolve("bin/pip").toString(), "install", "--quiet", "moto[server]==" + MOTO, "checkov==" + CHECKOV));
            Files.writeString(marker, "ok");
        }
    }

    Path bin(String name) {
        return dir.resolve("bin").resolve(name);
    }

    static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(900, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /** A running emulator on a free loopback port, stopped on close. */
    public static final class Moto implements AutoCloseable {

        private final Process process;
        private final int port;

        public Moto(Venv venv) throws IOException, InterruptedException {
            this.port = freePort();
            List<String> command = new ArrayList<>(List.of(venv.bin("moto_server").toString(), "-H", "127.0.0.1", "-p", String.valueOf(port)));
            this.process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            for (int i = 0; i < 100; i++) {
                try {
                    client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url() + "/moto-api/")).build(), java.net.http.HttpResponse.BodyHandlers.discarding());
                    return;
                } catch (IOException e) {
                    Thread.sleep(200);
                }
            }
            throw new IllegalStateException("moto did not start");
        }

        public String url() {
            return "http://127.0.0.1:" + port;
        }

        @Override
        public void close() {
            process.destroy();
            try {
                process.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
