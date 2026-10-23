package com.jk.explore.security.detectioncode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.json.JsonMapper;

/** One recorded log event: a number, a time in milliseconds and flat text fields named as Sigma names them (Image, CommandLine, User). */
record Ev(int id, long ms, Map<String, String> f) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    String json() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", id);
        doc.put("timestamp", Instant.ofEpochMilli(ms).toString());
        doc.putAll(new TreeMap<>(f));
        return JSON.writeValueAsString(doc);
    }
}
