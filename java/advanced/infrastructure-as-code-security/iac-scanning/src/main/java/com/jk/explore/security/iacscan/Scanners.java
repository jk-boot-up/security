package com.jk.explore.security.iacscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Checkov, Trivy and KICS reading Kubernetes manifests. What each reports is the set of check names it failed. */
public final class Scanners {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    static final String KICS_IMAGE = "checkmarx/kics@sha256:ef29dbac14b046b1ef65f3280646112e927a61781c5f01bca1aeb723e0b96003";  // was :latest; pinned so the golden findings depend on a fixed KICS ruleset

    private Scanners() {
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(300, TimeUnit.SECONDS);
        return out;
    }

    public static Path write(Path dir, String text) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("app.yaml"), text);
        return dir;
    }

    public static Set<String> checkov(Venv venv, Path dir) throws IOException, InterruptedException {
        JsonNode root = JSON.readTree(run(List.of(venv.bin("checkov").toString(), "-d", dir.toString(), "--framework", "kubernetes", "--quiet", "--compact", "-o", "json")));
        if (root.isArray()) {
            root = root.get(0);
        }
        Set<String> ids = new TreeSet<>();
        for (JsonNode c : root.get("results").get("failed_checks")) {
            ids.add(c.get("check_id").asString());
        }
        return ids;
    }

    public static Set<String> trivy(Path dir, String... extra) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("trivy", "config", "--format", "json", "--skip-version-check"));
        cmd.addAll(List.of(extra));
        cmd.add(dir.toString());
        Set<String> ids = new TreeSet<>();
        for (JsonNode r : JSON.readTree(run(cmd)).get("Results")) {
            if (r.has("Misconfigurations")) {
                for (JsonNode m : r.get("Misconfigurations")) {
                    if (m.get("Status").asString().equals("FAIL")) {
                        ids.add(m.get("ID").asString());
                    }
                }
            }
        }
        return ids;
    }

    public static Set<String> kics(Path dir) throws IOException, InterruptedException {
        Path out = Files.createTempDirectory(dir.toAbsolutePath().getParent(), "kics-");
        run(List.of("docker", "run", "--rm", "-v", dir.toAbsolutePath() + ":/src:ro", "-v", out + ":/out", KICS_IMAGE, "scan", "-p", "/src", "-o", "/out", "--report-formats", "json"));
        Set<String> names = new TreeSet<>();
        for (JsonNode q : JSON.readTree(out.resolve("results.json").toFile()).get("queries")) {
            names.add(q.get("query_name").asString());
        }
        return names;
    }

    /** For each of our rules, the check each tool has for the same problem (an empty list: the tool has none). */
    public static final Map<String, List<String>> CHECKOV = Map.of("K1", List.of("CKV_K8S_16"), "K2", List.of("CKV_K8S_23"), "K3", List.of("CKV_K8S_14", "CKV_K8S_43"),
            "K4", List.of("CKV_K8S_11", "CKV_K8S_13"), "K5", List.of(), "K6", List.of("CKV_K8S_19"), "K7", List.of("CKV_K8S_22"), "K8", List.of("CKV_K8S_20"));
    public static final Map<String, List<String>> TRIVY = Map.of("K1", List.of("KSV-0017"), "K2", List.of("KSV-0012"), "K3", List.of("KSV-0013"),
            "K4", List.of("KSV-0011", "KSV-0018"), "K5", List.of(), "K6", List.of("KSV-0009"), "K7", List.of("KSV-0014"), "K8", List.of("KSV-0001"));
    public static final Map<String, List<String>> KICS = Map.of("K1", List.of("Container Is Privileged"), "K2", List.of("Container Running As Root"), "K3", List.of("Invalid Image Tag"),
            "K4", List.of("CPU Limits Not Set", "Memory Limits Not Defined"), "K5", List.of(), "K6", List.of("Shared Host Network Namespace"),
            "K7", List.of("Root Container Not Mounted Read-only"), "K8", List.of("Privilege Escalation Allowed"));

    public static boolean catches(Set<String> failed, List<String> candidates) {
        return candidates.stream().anyMatch(failed::contains);
    }
}
