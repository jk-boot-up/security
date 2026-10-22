package com.jk.explore.security.searchdetect;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** A real OpenSearch 3.9 (with its alerting plugin) in a container on a free loopback port, with the security plugin off because this is a lab, removed on close. */
public final class Lab implements AutoCloseable {

    static final String IMAGE = "opensearchproject/opensearch:3.9.0";

    private final String container = "search-lab-" + UUID.randomUUID().toString().substring(0, 8);
    private final int port;

    public Lab() throws IOException, InterruptedException {
        run("docker", "run", "-d", "--rm", "--name", container, "-p", "127.0.0.1::9200", "-e", "discovery.type=single-node", "-e", "DISABLE_SECURITY_PLUGIN=true", "-e", "DISABLE_INSTALL_DEMO_CONFIG=true",
                "-e", "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m", IMAGE);
        try {
            String mapping = run("docker", "port", container, "9200").lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
            this.port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).strip());
            HttpClient client = HttpClient.newHttpClient();
            for (int i = 0; i < 240; i++) {
                try {
                    var r = client.send(HttpRequest.newBuilder(URI.create(url() + "/_cluster/health?wait_for_status=yellow&timeout=1s")).build(), HttpResponse.BodyHandlers.ofString());
                    if (r.statusCode() == 200 && r.body().contains("\"status\"")) {
                        return;
                    }
                } catch (IOException e) {
                    // not up yet
                }
                Thread.sleep(500);
            }
            throw new IllegalStateException("opensearch did not start: " + run("docker", "logs", "--tail", "20", container));
        } catch (IOException | InterruptedException | RuntimeException e) {
            close();
            throw e;
        }
    }

    String url() {
        return "http://127.0.0.1:" + port;
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    @Override
    public void close() {
        try {
            run("docker", "rm", "-f", container);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // already gone
        }
    }
}
