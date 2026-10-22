package com.jk.explore.security.searchdetect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/**
 * The detections as OpenSearch queries: filters, a terms aggregation for the subject, a date histogram for the window, and a pipeline aggregation that keeps only the buckets over the
 * threshold. Windows are fixed buckets, not sliding ones. The {@code attack} sub-aggregation reads the lab's ground truth for scoring and is never part of a rule.
 */
public final class Detections {

    static final Set<String> APPROVERS = Scenario.APPROVERS;

    private Detections() {
    }

    private static Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private static Map<String, Object> failures() {
        return Map.of("bool", Map.of("filter", List.of(term("type", "login"), term("success", false))));
    }

    private static Map<String, Object> attackAgg() {
        return Map.of("terms", Map.of("field", "attack"));
    }

    /** At least {@code n} failed logins for one account in a five-minute bucket. */
    public static Map<String, Object> bruteForce(int n) {
        return Map.of("size", 0, "query", failures(), "aggs", Map.of("subject", Map.of("terms", Map.of("field", "user", "size", 1000), "aggs",
                Map.of("window", Map.of("date_histogram", Map.of("field", "time", "fixed_interval", "5m", "min_doc_count", n), "aggs", Map.of("attack", attackAgg()))))));
    }

    /** Failed logins for at least {@code n} different accounts from one address in a twenty-minute bucket. */
    public static Map<String, Object> spraying(int n) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("date_histogram", Map.of("field", "time", "fixed_interval", "20m", "min_doc_count", 1));
        window.put("aggs", Map.of("accounts", Map.of("cardinality", Map.of("field", "user")), "attack", attackAgg(),
                "enough", Map.of("bucket_selector", Map.of("buckets_path", Map.of("u", "accounts"), "script", "params.u >= " + n))));
        return Map.of("size", 0, "query", failures(), "aggs", Map.of("subject", Map.of("terms", Map.of("field", "ip", "size", 1000), "aggs", Map.of("window", window))));
    }

    /** At least {@code n} failures and a success from one address in the same hour. The order is not checked: a query cannot say "then". */
    public static Map<String, Object> successAfterFailures(int n) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("date_histogram", Map.of("field", "time", "fixed_interval", "1h", "min_doc_count", 1));
        window.put("aggs", Map.of("failed", Map.of("filter", term("success", false)), "succeeded", Map.of("filter", term("success", true)), "attack", attackAgg(),
                "enough", Map.of("bucket_selector", Map.of("buckets_path", Map.of("f", "failed>_count", "s", "succeeded>_count"), "script", "params.f >= " + n + " && params.s >= 1"))));
        return Map.of("size", 0, "query", Map.of("term", Map.of("type", "login")), "aggs", Map.of("subject", Map.of("terms", Map.of("field", "ip", "size", 1000), "aggs", Map.of("window", window))));
    }

    /** Admin grants made by someone who is not an approver, or without a change ticket. */
    public static Map<String, Object> privilegeChange() {
        Map<String, Object> notApprover = Map.of("bool", Map.of("must_not", List.of(Map.of("terms", Map.of("actor", List.copyOf(APPROVERS))))));
        return Map.of("size", 100, "sort", List.of(Map.of("time", "asc")), "query", Map.of("bool", Map.of("filter", List.of(term("type", "role-grant"), term("role", "admin")),
                "should", List.of(notApprover, term("ticket", "")), "minimum_should_match", 1)));
    }

    /** Reads the buckets of a search with a subject terms aggregation and a window histogram under it. */
    public static List<Hit> buckets(JsonNode response, boolean requireWindowBucket) {
        List<Hit> hits = new ArrayList<>();
        for (JsonNode subject : response.get("aggregations").get("subject").get("buckets")) {
            for (JsonNode w : subject.get("window").get("buckets")) {
                Set<String> attacks = new TreeSet<>();
                w.get("attack").get("buckets").forEach(b -> attacks.add(b.get("key").asString()));
                hits.add(new Hit(subject.get("key").asString(), w.get("key_as_string").asString().substring(11, 16), w.get("doc_count").asLong(), attacks));
            }
        }
        return hits;
    }

    /** Reads the documents of a privilege-change search. */
    public static List<Hit> grants(JsonNode response) {
        List<Hit> hits = new ArrayList<>();
        for (JsonNode h : response.get("hits").get("hits")) {
            JsonNode s = h.get("_source");
            Set<String> attacks = new TreeSet<>();
            if (s.has("attack")) {
                attacks.add(s.get("attack").asString());
            }
            hits.add(new Hit(s.get("user").asString() + " by " + s.get("actor").asString() + (s.get("ticket").asString().isEmpty() ? ", no ticket" : ""), s.get("time").asString().substring(11, 16), 1, attacks));
        }
        return hits;
    }

    /** Runs a search on an index and reads it as alerts. */
    public static List<Hit> run(OpenSearch os, String index, Map<String, Object> search) throws IOException, InterruptedException {
        OpenSearch.Answer a = os.search(index, search);
        if (!a.ok()) {
            throw new IllegalStateException(a.json().toString());
        }
        return a.json().has("aggregations") ? buckets(a.json(), true) : grants(a.json());
    }
}
