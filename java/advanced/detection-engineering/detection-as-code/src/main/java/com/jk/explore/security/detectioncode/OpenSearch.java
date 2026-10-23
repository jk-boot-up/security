package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A single-node OpenSearch with the security plugin off, in a Docker container with a name of its own, on a free port; removed on close. */
final class OpenSearch implements AutoCloseable {

    static final String IMAGE = "opensearchproject/opensearch:3.9.0";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String INDEX = "bank-events";

    private final String container = "dac-os-" + ProcessHandle.current().pid() + "-" + (System.nanoTime() % 1_000_000);
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    OpenSearch() throws IOException, InterruptedException {
        int port = freePort();
        base = "http://localhost:" + port;
        Process p = new ProcessBuilder("docker", "run", "-d", "--name", container, "-p", port + ":9200", "-e", "discovery.type=single-node",
                "-e", "DISABLE_SECURITY_PLUGIN=true", "-e", "DISABLE_INSTALL_DEMO_CONFIG=true", "-e", "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m", IMAGE)
                .redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(900, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("docker run failed: " + out);
        }
        for (int i = 0; i < 180; i++) {
            try {
                if (send("GET", "/_cluster/health?wait_for_status=yellow&timeout=5s", null).statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(1000);
        }
        close();
        throw new IllegalStateException("OpenSearch did not start");
    }

    private static int freePort() throws IOException {
        for (int port = 5100; port < 5900; port++) {
            try (ServerSocket s = new ServerSocket(port)) {
                return s.getLocalPort();
            } catch (IOException e) {
                // taken
            }
        }
        throw new IOException("no free port");
    }

    private HttpResponse<String> send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Every string field is a keyword that is compared with no regard to case, which is how Sigma compares. */
    void index(Corpus c) throws IOException, InterruptedException {
        String mapping = """
                {"settings":{"number_of_shards":1,"number_of_replicas":0,"analysis":{"normalizer":{"lc":{"type":"custom","filter":["lowercase"]}}}},
                 "mappings":{"dynamic_templates":[{"strings":{"match_mapping_type":"string","mapping":{"type":"keyword","normalizer":"lc"}}}],
                 "properties":{"timestamp":{"type":"date"},"id":{"type":"integer"}}}}""";
        must(send("PUT", "/" + INDEX, mapping));
        StringBuilder bulk = new StringBuilder();
        for (Ev e : c.events()) {
            bulk.append("{\"index\":{\"_id\":\"").append(e.id()).append("\"}}\n").append(e.json()).append("\n");
        }
        JsonNode r = JSON.readTree(must(send("POST", "/" + INDEX + "/_bulk?refresh=true", bulk.toString())));
        if (r.get("errors").asBoolean()) {
            throw new IllegalStateException("bulk indexing had errors: " + r);
        }
    }

    /** The ids of the events a Lucene query string matches. */
    Set<Integer> hits(String query) throws IOException, InterruptedException {
        String body = JSON.writeValueAsString(Map.of("query", Map.of("query_string", Map.of("query", query)), "size", 10000, "_source", List.of("id")));
        JsonNode r = JSON.readTree(must(send("POST", "/" + INDEX + "/_search", body)));
        Set<Integer> ids = new TreeSet<>();
        r.get("hits").get("hits").forEach(h -> ids.add(h.get("_source").get("id").asInt()));
        return ids;
    }

    private static String must(HttpResponse<String> r) {
        if (r.statusCode() / 100 != 2) {
            throw new IllegalStateException("OpenSearch said " + r.statusCode() + ": " + r.body());
        }
        return r.body();
    }

    static boolean dockerRuns() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            new ProcessBuilder("docker", "rm", "-f", container).redirectErrorStream(true).start().waitFor(60, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            // best effort
        }
    }
}
