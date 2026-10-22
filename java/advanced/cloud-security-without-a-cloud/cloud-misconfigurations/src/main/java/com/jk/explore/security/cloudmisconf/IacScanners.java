package com.jk.explore.security.cloudmisconf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Checkov and Trivy reading OpenTofu/Terraform files: no cloud, no network after installation. */
public final class IacScanners {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** What a scanner reported for a folder: how many checks failed and the ids of the failures. */
    public record Report(int failed, Set<String> ids) {
    }

    private IacScanners() {
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(300, TimeUnit.SECONDS);
        return out;
    }

    public static Report checkov(Venv venv, Path dir) throws IOException, InterruptedException {
        String json = run(List.of(venv.bin("checkov").toString(), "-d", dir.toString(), "--framework", "terraform", "--quiet", "--compact", "-o", "json"));
        JsonNode root = JSON.readTree(json);
        if (root.isArray()) {
            root = root.get(0);
        }
        Set<String> ids = new TreeSet<>();
        int failed = 0;
        for (JsonNode c : root.get("results").get("failed_checks")) {
            ids.add(c.get("check_id").asString());
            failed++;
        }
        return new Report(failed, ids);
    }

    public static Report trivy(Path dir) throws IOException, InterruptedException {
        String json = run(List.of("trivy", "config", "--format", "json", "--skip-version-check", dir.toString()));
        Set<String> ids = new TreeSet<>();
        int failed = 0;
        for (JsonNode r : JSON.readTree(json).get("Results")) {
            if (r.has("Misconfigurations")) {
                for (JsonNode m : r.get("Misconfigurations")) {
                    ids.add(m.get("ID").asString());
                    failed++;
                }
            }
        }
        return new Report(failed, ids);
    }

    /** For each of our rules, the check ids that a scanner has for the same problem. */
    public static final java.util.Map<String, List<String>> CHECKOV = java.util.Map.of("s3-public-acl", List.of("CKV_AWS_20"), "s3-public-policy", List.of("CKV_AWS_70"),
            "s3-no-public-access-block", List.of("CKV2_AWS_6", "CKV_AWS_53", "CKV_AWS_54", "CKV_AWS_55", "CKV_AWS_56"), "s3-no-encryption", List.of("CKV_AWS_145", "CKV_AWS_19"), "s3-no-versioning", List.of("CKV_AWS_21"),
            "iam-wildcard-admin", List.of("CKV_AWS_355", "CKV_AWS_62", "CKV_AWS_63"), "sg-world-admin-port", List.of("CKV_AWS_24", "CKV_AWS_25"), "ec2-imdsv1", List.of("CKV_AWS_79"), "ebs-unencrypted", List.of("CKV_AWS_3"));

    public static final java.util.Map<String, List<String>> TRIVY = java.util.Map.of("s3-public-acl", List.of("AWS-0092"), "s3-public-policy", List.of(),
            "s3-no-public-access-block", List.of("AWS-0086", "AWS-0087", "AWS-0091", "AWS-0093", "AWS-0094"), "s3-no-encryption", List.of("AWS-0132", "AWS-0088"), "s3-no-versioning", List.of("AWS-0090"),
            "iam-wildcard-admin", List.of("AWS-0057"), "sg-world-admin-port", List.of("AWS-0107"), "ec2-imdsv1", List.of("AWS-0028"), "ebs-unencrypted", List.of("AWS-0026", "AWS-0131"));

    public static List<String> catches(Set<String> failedIds, List<String> candidates) {
        List<String> out = new ArrayList<>();
        for (String c : candidates) {
            if (failedIds.contains(c)) {
                out.add(c);
            }
        }
        return out;
    }
}
