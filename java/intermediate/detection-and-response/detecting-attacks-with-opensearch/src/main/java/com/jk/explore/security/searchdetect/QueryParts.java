package com.jk.explore.security.searchdetect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads a detection query back as the four parts every rule has: a filter (which events), a group by (whose events), a bucket (which stretch of time) and a
 * threshold (how many are worth an alert). It reads the same maps that {@link Detections} sends, so what the demo prints is what OpenSearch receives.
 */
public final class QueryParts {

    private QueryParts() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> at(Object node, String... path) {
        Object n = node;
        for (String key : path) {
            n = n instanceof Map<?, ?> m ? m.get(key) : null;
        }
        return (Map<String, Object>) n;
    }

    /** The four parts of a bucket query from {@link Detections}, in the order OpenSearch applies them. */
    public static Map<String, String> of(Map<String, Object> search) {
        Map<String, String> parts = new LinkedHashMap<>();
        parts.put("filter", filter(at(search, "query")));
        Map<String, Object> subject = at(search, "aggs", "subject");
        parts.put("group by", at(subject, "terms").get("field") + " (terms)");
        Map<String, Object> histogram = at(subject, "aggs", "window", "date_histogram");
        parts.put("bucket", histogram.get("fixed_interval") + " (date_histogram)");
        int minCount = (Integer) histogram.get("min_doc_count");
        Map<String, Object> selector = at(subject, "aggs", "window", "aggs", "enough", "bucket_selector");
        parts.put("threshold", selector != null ? selector.get("script").toString() : "min_doc_count " + minCount);
        return parts;
    }

    @SuppressWarnings("unchecked")
    private static String filter(Map<String, Object> query) {
        List<Map<String, Object>> terms = query.containsKey("term") ? List.of(query) : (List<Map<String, Object>>) at(query, "bool").get("filter");
        return terms.stream().map(t -> at(t, "term").entrySet().iterator().next()).map(e -> e.getKey() + " = " + e.getValue()).collect(Collectors.joining(" and "));
    }
}
