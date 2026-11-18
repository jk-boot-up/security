package com.jk.explore.security.dast;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** OWASP ZAP in Docker, driven by an automation plan: spider, passive rules, and optionally an active scan limited to chosen rules. */
final class Zap {

    static final String IMAGE = "ghcr.io/zaproxy/zaproxy@sha256:5ccfff9a411743ab2615136a2edd7416bec9ce41572fa94e935df38c14adb2b6";  // was :stable; pinned so the scan is reproducible (the golden output depends on ZAP's rules)

    /** The plan as YAML. {@code session} is a cookie to send with every request, or null; {@code activeRules} are ZAP rule ids, empty for no active scan. */
    static String plan(String target, String session, List<Integer> activeRules) {
        StringBuilder y = new StringBuilder();
        String host = URI.create(target).getAuthority();
        y.append("env:\n  contexts:\n    - name: bank\n      urls: [\"").append(target).append("\"]\n      includePaths: [\"http://").append(host.replace(".", "\\\\.")).append(".*\"]\n");
        y.append("  parameters: {failOnError: true, progressToStdout: false}\njobs:\n");
        if (session != null) {
            y.append("  - type: replacer\n    parameters: {deleteAllRules: true}\n");
            y.append("    rules:\n      - description: session\n");
            y.append("        matchType: req_header\n        matchString: Cookie\n");
            y.append("        replacementString: session=").append(session).append("\n");
        }
        y.append("  - type: spider\n    parameters: {maxDuration: 1}\n  - type: passiveScan-wait\n");
        if (!activeRules.isEmpty()) {
            y.append("  - type: activeScan\n    parameters: {maxScanDurationInMins: 3, maxRuleDurationInMins: 1}\n    policyDefinition:\n      defaultStrength: low\n      defaultThreshold: \"off\"\n      rules:\n");
            for (int id : activeRules) {
                y.append("        - {id: ").append(id).append(", threshold: medium, strength: medium}\n");
            }
        }
        y.append("  - type: report\n    parameters: {template: traditional-json, reportDir: /zap/wrk, reportFile: report.json}\n");
        return y.toString();
    }

    /** Runs a scan of the target and returns the alerts of the report. */
    static List<Alert> scan(String target, String session, List<Integer> activeRules) throws IOException, InterruptedException {
        Path work = Files.createTempDirectory("zap");
        Files.setPosixFilePermissions(work, PosixFilePermissions.fromString("rwxrwxrwx"));
        Files.writeString(work.resolve("plan.yaml"), plan(target, session, activeRules));
        Sh.must(List.of("docker", "run", "--rm", "-v", work + ":/zap/wrk:rw", IMAGE, "zap.sh", "-cmd", "-autorun", "/zap/wrk/plan.yaml"));
        return parse(Files.readString(work.resolve("report.json")));
    }

    /** The alerts of a ZAP traditional-json report, one for each name, with the distinct places they were raised. */
    static List<Alert> parse(String json) {
        Map<String, Alert> byName = new TreeMap<>();
        for (JsonNode site : new JsonMapper().readTree(json).path("site")) {
            for (JsonNode a : site.path("alerts")) {
                List<Alert.Hit> hits = new ArrayList<>();
                for (JsonNode i : a.path("instances")) {
                    String path = URI.create(i.path("uri").asString()).getPath();
                    hits.add(new Alert.Hit(path == null || path.isEmpty() ? "/" : path, i.path("param").asString(), i.path("attack").asString()));
                }
                String name = a.path("name").asString();
                Alert alert = new Alert(name, a.path("riskcode").asInt(), a.path("confidence").asInt(), hits);
                byName.merge(name, alert, (x, y) -> new Alert(name, x.risk(), x.confidence(), Stream.concat(x.hits().stream(), y.hits().stream()).toList()));
            }
        }
        List<Alert> alerts = new ArrayList<>();
        for (Alert a : byName.values()) {
            alerts.add(new Alert(a.name(), a.risk(), a.confidence(),
                    a.hits().stream().distinct().sorted(Comparator.comparing(Alert.Hit::path).thenComparing(Alert.Hit::param).thenComparing(Alert.Hit::attack)).toList()));
        }
        return alerts;
    }
}
