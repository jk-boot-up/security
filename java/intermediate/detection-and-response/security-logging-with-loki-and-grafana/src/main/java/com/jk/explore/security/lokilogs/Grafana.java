package com.jk.explore.security.lokilogs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Grafana's HTTP API: a Loki data source for one tenant, and a query sent through it. */
public final class Grafana {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String url;
    private final String auth = "Basic " + Base64.getEncoder().encodeToString("admin:lab-only".getBytes(StandardCharsets.UTF_8));

    public Grafana(String url) {
        this.url = url;
    }

    private JsonNode call(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + path)).header("Authorization", auth).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return JSON.readTree(r.body());
    }

    /** Adds a Loki data source that sends the tenant header, and returns its uid. */
    public String addLoki(String name, String tenant) throws IOException, InterruptedException {
        Map<String, Object> source = Map.of("name", name, "type", "loki", "access", "proxy", "url", "http://loki:3100", "jsonData", Map.of("httpHeaderName1", "X-Scope-OrgID"),
                "secureJsonData", Map.of("httpHeaderValue1", tenant));
        return call("POST", "/api/datasources", JSON.writeValueAsString(source)).get("datasource").get("uid").asString();
    }

    public String health(String uid) throws IOException, InterruptedException {
        return call("GET", "/api/datasources/uid/" + uid + "/health", null).get("status").asString();
    }

    /** How many lines a LogQL query returns when Grafana asks Loki for it. */
    public int count(String uid, String logql) throws IOException, InterruptedException {
        long now = Instant.now().toEpochMilli();
        Map<String, Object> query = Map.of("refId", "A", "datasource", Map.of("uid", uid, "type", "loki"), "expr", logql, "queryType", "range", "maxLines", 5000);
        String body = JSON.writeValueAsString(Map.of("queries", List.of(query), "from", String.valueOf(now - 3_600_000), "to", String.valueOf(now + 60_000)));
        JsonNode frames = call("POST", "/api/ds/query", body).get("results").get("A").get("frames");
        int total = 0;
        for (JsonNode frame : frames) {
            JsonNode values = frame.get("data").get("values");
            if (values != null && values.size() > 2) {
                total += values.get(2).size();
            }
        }
        return total;
    }
}
