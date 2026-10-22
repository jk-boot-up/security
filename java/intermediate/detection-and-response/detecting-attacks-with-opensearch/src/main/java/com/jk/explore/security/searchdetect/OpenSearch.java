package com.jk.explore.security.searchdetect;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** OpenSearch's HTTP API: indices, bulk loading, searches, and the alerting plugin's monitors. */
public final class OpenSearch {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** What came back: the status and the body as JSON. */
    public record Answer(int status, JsonNode json) {

        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final String url;

    public OpenSearch(String url) {
        this.url = url;
    }

    Answer call(String method, String path, String body, String contentType) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + path)).header("Content-Type", contentType);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Answer(r.statusCode(), r.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(r.body()));
    }

    Answer call(String method, String path, Object body) throws IOException, InterruptedException {
        return call(method, path, body == null ? null : JSON.writeValueAsString(body), "application/json");
    }

    /** An index with the mapping the rules rely on: the fields they filter and group by are keywords, the time is a date. */
    public void createAuditIndex(String index) throws IOException, InterruptedException {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("time", Map.of("type", "date"));
        for (String keyword : List.of("type", "user", "ip", "city", "actor", "role", "ticket", "attack")) {
            props.put(keyword, Map.of("type", "keyword"));
        }
        props.put("success", Map.of("type", "boolean"));
        Answer a = call("PUT", "/" + index, Map.of("settings", Map.of("number_of_shards", 1, "number_of_replicas", 0), "mappings", Map.of("properties", props)));
        if (!a.ok()) {
            throw new IllegalStateException(a.json().toString());
        }
    }

    static Map<String, Object> doc(Event e) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("time", e.time().toString());
        d.put("type", e.type());
        d.put("user", e.user());
        d.put("ip", e.ip());
        d.put("city", e.city().name());
        d.put("success", e.success());
        d.put("actor", e.actor());
        d.put("role", e.role());
        d.put("ticket", e.ticket());
        if (e.attackId() != null) {
            d.put("attack", e.attackId());
        }
        return d;
    }

    /** Loads the events with the bulk API and refreshes the index so that they can be searched at once. */
    public int bulk(String index, List<Event> events) throws IOException, InterruptedException {
        StringBuilder body = new StringBuilder();
        for (Event e : events) {
            body.append("{\"index\":{}}\n").append(JSON.writeValueAsString(doc(e))).append('\n');
        }
        Answer a = call("POST", "/" + index + "/_bulk?refresh=true", body.toString(), "application/x-ndjson");
        if (a.json().get("errors").asBoolean()) {
            throw new IllegalStateException(a.json().toString());
        }
        return a.json().get("items").size();
    }

    public Answer search(String index, Object body) throws IOException, InterruptedException {
        return call("POST", "/" + index + "/_search", body);
    }

    public long count(String index, Object query) throws IOException, InterruptedException {
        return call("POST", "/" + index + "/_count", Map.of("query", query)).json().get("count").asLong();
    }

    /** Every event of the index in time order, rebuilt as events, read in pages with search_after. */
    public List<Event> events(String index) throws IOException, InterruptedException {
        List<Event> out = new ArrayList<>();
        List<Object> after = null;
        while (true) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("size", 500);
            body.put("sort", List.of(Map.of("time", "asc"), Map.of("user", "asc"), Map.of("ip", "asc")));
            if (after != null) {
                body.put("search_after", after);
            }
            JsonNode hits = search(index, body).json().get("hits").get("hits");
            if (hits.size() == 0) {
                return out;
            }
            for (JsonNode h : hits) {
                JsonNode s = h.get("_source");
                out.add(new Event(java.time.Instant.parse(s.get("time").asString()), s.get("type").asString(), s.get("user").asString(), s.get("ip").asString(), City.byName(s.get("city").asString()),
                        s.get("success").asBoolean(), s.get("actor").asString(), s.get("role").asString(), s.get("ticket").asString(), s.has("attack") ? s.get("attack").asString() : null));
            }
            after = new ArrayList<>();
            for (JsonNode v : hits.get(hits.size() - 1).get("sort")) {
                after.add(v.isNumber() ? v.asLong() : v.asString());
            }
        }
    }

    /** Creates a monitor and returns its id. */
    public String createMonitor(Object monitor) throws IOException, InterruptedException {
        Answer a = call("POST", "/_plugins/_alerting/monitors", monitor);
        if (!a.ok()) {
            throw new IllegalStateException(a.json().toString());
        }
        return a.json().get("_id").asString();
    }

    /** Runs a monitor once without sending anything and returns what the trigger found. */
    public JsonNode execute(String monitorId) throws IOException, InterruptedException {
        return call("POST", "/_plugins/_alerting/monitors/" + monitorId + "/_execute?dryrun=true", null).json();
    }

    public int monitors() throws IOException, InterruptedException {
        return call("POST", "/_plugins/_alerting/monitors/_search", Map.of("query", Map.of("match_all", Map.of()))).json().get("hits").get("total").get("value").asInt();
    }
}
