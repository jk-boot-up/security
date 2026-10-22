package com.jk.explore.security.sbomscan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;

/** What grype found in an image or in a bill of materials. */
record Scan(List<Finding> findings) {

    /** One advisory against one package. {@code alias} is the CVE id when the advisory's own id is a GitHub one. */
    record Finding(String packageName, String version, String id, String alias, String severity, List<String> fixedIn) {

        boolean severe() {
            return severity.equals("Critical") || severity.equals("High");
        }
    }

    static Scan ofImage(String image) throws IOException, InterruptedException {
        return parse(Lab.run(List.of("grype", image, "-o", "json", "-q")));
    }

    /** Scans a bill of materials that was written earlier, not the image. */
    static Scan ofSbom(java.nio.file.Path sbom) throws IOException, InterruptedException {
        return parse(Lab.run(List.of("grype", "sbom:" + sbom, "-o", "json", "-q")));
    }

    @SuppressWarnings("unchecked")
    static Scan parse(String json) {
        Map<String, Object> root = Sbom.JSON.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        List<Finding> out = new ArrayList<>();
        for (Map<String, Object> m : (List<Map<String, Object>>) root.get("matches")) {
            Map<String, Object> vulnerability = (Map<String, Object>) m.get("vulnerability");
            Map<String, Object> artifact = (Map<String, Object>) m.get("artifact");
            List<Map<String, Object>> related = (List<Map<String, Object>>) m.getOrDefault("relatedVulnerabilities", List.of());
            Map<String, Object> fix = (Map<String, Object>) vulnerability.get("fix");
            out.add(new Finding(String.valueOf(artifact.get("name")), String.valueOf(artifact.get("version")), String.valueOf(vulnerability.get("id")),
                    related.isEmpty() ? "" : String.valueOf(related.get(0).get("id")), String.valueOf(vulnerability.get("severity")), (List<String>) fix.getOrDefault("versions", List.of())));
        }
        return new Scan(out);
    }

    List<Finding> inPackages(String prefix) {
        return findings.stream().filter(f -> f.packageName().startsWith(prefix)).toList();
    }

    TreeSet<String> ids(String packagePrefix) {
        TreeSet<String> ids = new TreeSet<>();
        inPackages(packagePrefix).forEach(f -> ids.add(f.id()));
        return ids;
    }
}
