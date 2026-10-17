package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads the JSON that `trivy sbom --format json` and `trivy fs --format json` write: Results, each with a list of Vulnerabilities. */
final class TrivyReport {

    private TrivyReport() {
    }

    static String version(String json) {
        Map<String, Object> trivy = Json.object(Json.object(Json.parse(json)).get("Trivy"));
        return Json.string(trivy.get("Version"));
    }

    static List<Vulnerability> parse(String json) {
        Map<String, Object> document = Json.object(Json.parse(json));
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        Object results = document.get("Results");
        if (results == null) {
            return vulnerabilities;
        }
        for (Object result : Json.array(results)) {
            Object found = Json.object(result).get("Vulnerabilities");
            if (found == null) {
                continue;
            }
            for (Object entry : Json.array(found)) {
                Map<String, Object> v = Json.object(entry);
                vulnerabilities.add(new Vulnerability(
                        Json.string(v.get("VulnerabilityID")), Json.string(v.get("PkgName")), Json.string(v.get("InstalledVersion")),
                        v.get("FixedVersion") == null ? "" : Json.string(v.get("FixedVersion")),
                        Json.string(v.get("Severity")), v.get("Title") == null ? "" : Json.string(v.get("Title"))));
            }
        }
        return vulnerabilities;
    }
}
