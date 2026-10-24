package com.jk.explore.security.dast;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
            y.append("    rules:\n      - description: session\n        matchType: req_header\n        matchString: Cookie\n        replacementString: session=").append(session).append("\n");
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

    /** The alerts of a ZAP traditional-json report, one for each name, with the paths they were raised on. */
    static List<Alert> parse(String json) {
        Map<String, Alert> byName = new TreeMap<>();
        for (JsonNode site : new JsonMapper().readTree(json).path("site")) {
            for (JsonNode a : site.path("alerts")) {
                List<String> paths = new ArrayList<>();
                for (JsonNode i : a.path("instances")) {
                    String path = URI.create(i.path("uri").asString()).getPath();
                    paths.add(path == null || path.isEmpty() ? "/" : path);
                }
                String name = a.path("name").asString();
                int risk = a.path("riskcode").asInt();
                byName.merge(name, new Alert(name, risk, paths.stream().distinct().sorted().toList()), (x, y) -> new Alert(name, risk, java.util.stream.Stream.concat(x.paths().stream(), y.paths().stream()).distinct().sorted().toList()));
            }
        }
        return new ArrayList<>(byName.values());
    }
}
