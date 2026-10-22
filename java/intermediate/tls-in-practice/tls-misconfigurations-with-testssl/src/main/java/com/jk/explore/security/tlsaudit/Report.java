package com.jk.explore.security.tlsaudit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** What testssl.sh wrote in its JSON file: one finding per check, each with the severity testssl gives it. */
record Report(Map<String, Finding> findings) {

    record Finding(String id, String severity, String text) {
    }

    static final JsonMapper JSON = JsonMapper.builder().build();

    static Report parse(String json) {
        List<Map<String, Object>> rows = JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() {
        });
        Map<String, Finding> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            out.putIfAbsent(id, new Finding(id, String.valueOf(row.get("severity")), String.valueOf(row.get("finding"))));
        }
        return new Report(out);
    }

    Finding get(String id) {
        return findings.get(id);
    }

    String severity(String id) {
        Finding f = findings.get(id);
        return f == null ? "MISSING" : f.severity();
    }

    String text(String id) {
        Finding f = findings.get(id);
        return f == null ? "" : f.text();
    }

    boolean offered(String protocolId) {
        return text(protocolId).startsWith("offered");
    }

    /** How many findings there are at each severity. */
    Map<String, Integer> bySeverity() {
        Map<String, Integer> out = new TreeMap<>();
        findings.values().forEach(f -> out.merge(f.severity(), 1, Integer::sum));
        return out;
    }

    /** The ids that testssl rates at one of these severities, sorted. */
    List<String> idsAt(String... severities) {
        List<String> wanted = List.of(severities);
        return findings.values().stream().filter(f -> wanted.contains(f.severity())).map(Finding::id).sorted().toList();
    }
}
