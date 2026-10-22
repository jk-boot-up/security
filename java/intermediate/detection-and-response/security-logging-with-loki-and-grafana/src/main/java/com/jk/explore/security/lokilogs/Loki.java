package com.jk.explore.security.lokilogs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Loki's HTTP API, for one tenant: push lines, ask LogQL questions, list labels and streams, read the alerts the rule evaluator holds. */
public final class Loki {

    /** One line that came back: the stream's labels, the time in nanoseconds, and the text. */
    public record Line(Map<String, String> labels, long nanos, String text) {
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String url;
    private final String tenant;

    public Loki(String url, String tenant) {
        this.url = url;
        this.tenant = tenant;
    }

    /** What the server answered and how. */
    public record Answer(int status, String body) {

        JsonNode json() {
            return JSON.readTree(body);
        }
    }

    private Answer send(HttpRequest.Builder request) throws IOException, InterruptedException {
        if (tenant != null) {
            request.header("X-Scope-OrgID", tenant);
        }
        HttpResponse<String> r = client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Answer(r.statusCode(), r.body());
    }

    /** Pushes one line to the stream with these labels, stamped {@code at}. */
    public Answer push(Map<String, String> labels, Instant at, String line) throws IOException, InterruptedException {
        long nanos = at.getEpochSecond() * 1_000_000_000L + at.getNano();
        String body = JSON.writeValueAsString(Map.of("streams", List.of(Map.of("stream", labels, "values", List.of(List.of(String.valueOf(nanos), line))))));
        return send(HttpRequest.newBuilder(URI.create(url + "/loki/api/v1/push")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    /** Pushes many lines of one stream in a single request. */
    public Answer pushAll(Map<String, String> labels, List<Instant> times, List<String> lines) throws IOException, InterruptedException {
        List<List<String>> values = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            values.add(List.of(String.valueOf(times.get(i).getEpochSecond() * 1_000_000_000L + times.get(i).getNano()), lines.get(i)));
        }
        String body = JSON.writeValueAsString(Map.of("streams", List.of(Map.of("stream", labels, "values", values))));
        return send(HttpRequest.newBuilder(URI.create(url + "/loki/api/v1/push")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private Answer get(String path, String... params) throws IOException, InterruptedException {
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < params.length; i += 2) {
            q.append(q.length() == 0 ? "?" : "&").append(params[i]).append('=').append(URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8));
        }
        return send(HttpRequest.newBuilder(URI.create(url + path + q)));
    }

    /** The lines that a LogQL log query returns, oldest first, over the last hour. */
    public List<Line> lines(String logql) throws IOException, InterruptedException {
        Answer a = get("/loki/api/v1/query_range", "query", logql, "limit", "5000", "direction", "forward", "start", String.valueOf(Instant.now().minusSeconds(3600).getEpochSecond() * 1_000_000_000L),
                "end", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond() * 1_000_000_000L));
        List<Line> out = new ArrayList<>();
        for (JsonNode result : a.json().get("data").get("result")) {
            Map<String, String> labels = new TreeMap<>();
            result.get("stream").propertyStream().forEach(e -> labels.put(e.getKey(), e.getValue().asString()));
            for (JsonNode v : result.get("values")) {
                out.add(new Line(labels, Long.parseLong(v.get(0).asString()), v.get(1).asString()));
            }
        }
        out.sort(java.util.Comparator.comparingLong(Line::nanos));
        return out;
    }

    /** The result of a LogQL metric query, such as a count by address, as label value to number (the label named {@code by}). */
    public Map<String, Double> metric(String logql, String by) throws IOException, InterruptedException {
        Answer a = get("/loki/api/v1/query", "query", logql, "time", String.valueOf(Instant.now().getEpochSecond()));
        Map<String, Double> out = new TreeMap<>();
        for (JsonNode r : a.json().get("data").get("result")) {
            out.put(r.get("metric").get(by).asString(), Double.parseDouble(r.get("value").get(1).asString()));
        }
        return out;
    }

    public List<String> labels() throws IOException, InterruptedException {
        List<String> out = new ArrayList<>();
        JsonNode data = get("/loki/api/v1/labels").json().get("data");
        if (data != null) {
            data.forEach(n -> out.add(n.asString()));
        }
        return out.stream().sorted().toList();
    }

    /** How many distinct streams (label sets) the tenant has. */
    public int streams(String selector) throws IOException, InterruptedException {
        JsonNode data = get("/loki/api/v1/series", "match[]", selector).json().get("data");
        return data == null ? 0 : data.size();
    }

    /** The alerts that the rule evaluator holds for this tenant, as alert name plus the value of the label {@code source_ip}, firing only. */
    public List<String> firingAlerts() throws IOException, InterruptedException {
        List<String> out = new ArrayList<>();
        for (JsonNode alert : get("/prometheus/api/v1/alerts").json().get("data").get("alerts")) {
            if (alert.get("state").asString().equals("firing")) {
                out.add(alert.get("labels").get("alertname").asString() + " " + alert.get("labels").get("source_ip").asString());
            }
        }
        return out.stream().sorted().toList();
    }

    /** Each rule of the tenant with the state the evaluator has for it: firing, pending or inactive. */
    public Map<String, String> ruleStates() throws IOException, InterruptedException {
        Map<String, String> out = new TreeMap<>();
        for (JsonNode group : get("/prometheus/api/v1/rules").json().get("data").get("groups")) {
            for (JsonNode rule : group.get("rules")) {
                out.put(rule.get("name").asString(), rule.get("state").asString() + "/" + rule.get("health").asString());
            }
        }
        return out;
    }

    public String summaryOfFirstAlert() throws IOException, InterruptedException {
        return get("/prometheus/api/v1/alerts").json().get("data").get("alerts").get(0).get("annotations").get("summary").asString();
    }

    public Answer deleteLines(String selector) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(url + "/loki/api/v1/delete?query=" + URLEncoder.encode(selector, StandardCharsets.UTF_8) + "&start=" + (Instant.now().minusSeconds(86400).getEpochSecond()))).POST(HttpRequest.BodyPublishers.noBody()));
    }
}
