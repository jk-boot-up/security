package com.jk.explore.security.searchdetect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The same detections as monitors of OpenSearch's alerting plugin: a schedule, the query, and a trigger that decides which buckets are alerts. */
public final class Monitors {

    private Monitors() {
    }

    private static Map<String, Object> monitor(String name, String type, Map<String, Object> query, Map<String, Object> trigger) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "monitor");
        m.put("name", name);
        m.put("monitor_type", type);
        m.put("enabled", true);
        m.put("schedule", Map.of("period", Map.of("interval", 1, "unit", "MINUTES")));
        m.put("inputs", List.of(Map.of("search", Map.of("indices", List.of("audit"), "query", query))));
        m.put("triggers", List.of(trigger));
        return m;
    }

    private static Map<String, Object> bucketTrigger(String name, Map<String, String> paths, String script) {
        return Map.of("bucket_level_trigger", Map.of("name", name, "severity", "1", "condition", Map.of("buckets_path", paths, "parent_bucket_path", "composite_agg", "script", Map.of("source", script)), "actions", List.of()));
    }

    private static Map<String, Object> failures() {
        return Map.of("bool", Map.of("filter", List.of(Map.of("term", Map.of("type", "login")), Map.of("term", Map.of("success", false)))));
    }

    private static Map<String, Object> composite(String field, String interval, Map<String, Object> subAggs) {
        Map<String, Object> agg = new LinkedHashMap<>();
        agg.put("composite", Map.of("size", 1000, "sources", List.of(Map.of("subject", Map.of("terms", Map.of("field", field))), Map.of("window", Map.of("date_histogram", Map.of("field", "time", "fixed_interval", interval))))));
        agg.put("aggs", subAggs);
        return Map.of("size", 0, "query", failures(), "aggs", Map.of("composite_agg", agg));
    }

    public static Map<String, Object> bruteForce(int n) {
        return monitor("brute-force(" + n + ")", "bucket_level_monitor", composite("user", "5m", Map.of("attack", Map.of("terms", Map.of("field", "attack")))), bucketTrigger("at least " + n + " failures", Map.of("_count", "_count"), "params._count >= " + n));
    }

    public static Map<String, Object> spraying(int n) {
        return monitor("spraying(" + n + ")", "bucket_level_monitor", composite("ip", "20m", Map.of("accounts", Map.of("cardinality", Map.of("field", "user")), "attack", Map.of("terms", Map.of("field", "attack")))),
                bucketTrigger("at least " + n + " accounts", Map.of("u", "accounts"), "params.u >= " + n));
    }

    public static Map<String, Object> privilegeChange() {
        Map<String, Object> query = Detections.privilegeChange();
        return monitor("privilege-change", "query_level_monitor", query, Map.of("query_level_trigger", Map.of("name", "an admin grant without approval", "severity", "1",
                "condition", Map.of("script", Map.of("source", "ctx.results[0].hits.total.value > 0", "lang", "painless")), "actions", List.of())));
    }

    /** What a bucket-level monitor's trigger found: for each triggered bucket, its subject, its window and the attacks behind it, from the bucket the trigger reports. */
    public static java.util.List<Hit> triggered(tools.jackson.databind.JsonNode run) {
        java.util.List<Hit> hits = new java.util.ArrayList<>();
        tools.jackson.databind.JsonNode trigger = run.get("trigger_results").propertyStream().findFirst().orElseThrow().getValue();
        for (var entry : (Iterable<java.util.Map.Entry<String, tools.jackson.databind.JsonNode>>) trigger.get("agg_result_buckets").properties()) {
            tools.jackson.databind.JsonNode b = entry.getValue().get("agg_alert_content").get("bucket");
            java.util.Set<String> attacks = new java.util.TreeSet<>();
            b.get("attack").get("buckets").forEach(x -> attacks.add(x.get("key").asString()));
            String window = java.time.Instant.ofEpochMilli(b.get("key").get("window").asLong()).toString().substring(11, 16);
            hits.add(new Hit(b.get("key").get("subject").asString(), window, b.get("doc_count").asLong(), attacks));
        }
        hits.sort(java.util.Comparator.comparing(Hit::window).thenComparing(Hit::subject));
        return hits;
    }

    public static Map<String, Object> successAfterFailures(int n) {
        Map<String, Object> query = Map.of("term", Map.of("type", "login"));
        Map<String, Object> agg = new LinkedHashMap<>();
        agg.put("composite", Map.of("size", 1000, "sources", List.of(Map.of("subject", Map.of("terms", Map.of("field", "ip"))), Map.of("window", Map.of("date_histogram", Map.of("field", "time", "fixed_interval", "1h"))))));
        agg.put("aggs", Map.of("failed", Map.of("filter", Map.of("term", Map.of("success", false))), "succeeded", Map.of("filter", Map.of("term", Map.of("success", true))), "attack", Map.of("terms", Map.of("field", "attack"))));
        return monitor("success-after-failures(" + n + ")", "bucket_level_monitor", Map.of("size", 0, "query", query, "aggs", Map.of("composite_agg", agg)),
                bucketTrigger("at least " + n + " failures and a success", Map.of("f", "failed>_count", "s", "succeeded>_count"), "params.f >= " + n + " && params.s >= 1"));
    }
}
