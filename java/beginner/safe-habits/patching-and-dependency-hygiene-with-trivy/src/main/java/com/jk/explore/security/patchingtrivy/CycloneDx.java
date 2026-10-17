package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes and checks a software bill of materials in the CycloneDX 1.6 JSON format, which scanners such as Trivy read.
 * The serial number and time stamp are fixed so that the file is the same every time.
 */
final class CycloneDx {

    static final String SPEC_VERSION = "1.6";
    static final String SERIAL_NUMBER = "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79";
    static final String TIMESTAMP = "2026-01-01T00:00:00Z";

    private CycloneDx() {
    }

    static String write(String application, String applicationVersion, List<Dependency> dependencies) {
        String appRef = "app:" + application + "@" + applicationVersion;
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"bomFormat\": \"CycloneDX\",\n");
        out.append("  \"specVersion\": \"").append(SPEC_VERSION).append("\",\n");
        out.append("  \"serialNumber\": \"").append(SERIAL_NUMBER).append("\",\n");
        out.append("  \"version\": 1,\n");
        out.append("  \"metadata\": {\n    \"timestamp\": \"").append(TIMESTAMP).append("\",\n");
        out.append("    \"component\": {\"type\": \"application\", \"bom-ref\": ").append(quote(appRef))
                .append(", \"name\": ").append(quote(application)).append(", \"version\": ").append(quote(applicationVersion)).append("}\n  },\n");
        out.append("  \"components\": [\n");
        for (int i = 0; i < dependencies.size(); i++) {
            Dependency d = dependencies.get(i);
            out.append("    {\"type\": \"library\", \"bom-ref\": ").append(quote(d.purl()))
                    .append(", \"group\": ").append(quote(d.group()))
                    .append(", \"name\": ").append(quote(d.artifact()))
                    .append(", \"version\": ").append(quote(d.version()))
                    .append(", \"purl\": ").append(quote(d.purl())).append('}')
                    .append(i + 1 < dependencies.size() ? ",\n" : "\n");
        }
        out.append("  ],\n  \"dependencies\": [\n");
        out.append("    {\"ref\": ").append(quote(appRef)).append(", \"dependsOn\": [");
        for (int i = 0; i < dependencies.size(); i++) {
            out.append(i > 0 ? ", " : "").append(quote(dependencies.get(i).purl()));
        }
        out.append("]}\n  ]\n}\n");
        return out.toString();
    }

    /** Checks the parts a scanner needs, and that every package URL agrees with the group, name and version beside it. Returns the libraries. */
    static List<Dependency> read(String json) {
        Map<String, Object> document = Json.object(Json.parse(json));
        if (!"CycloneDX".equals(document.get("bomFormat"))) {
            throw new IllegalArgumentException("bomFormat is not CycloneDX");
        }
        if (!SPEC_VERSION.equals(document.get("specVersion"))) {
            throw new IllegalArgumentException("specVersion is not " + SPEC_VERSION);
        }
        Set<String> refs = new HashSet<>();
        List<Dependency> libraries = new ArrayList<>();
        for (Object entry : Json.array(document.get("components"))) {
            Map<String, Object> component = Json.object(entry);
            Dependency dependency = new Dependency(Json.string(component.get("group")), Json.string(component.get("name")), Json.string(component.get("version")));
            if (!dependency.purl().equals(component.get("purl"))) {
                throw new IllegalArgumentException("the package URL of " + dependency.coordinates() + " does not match its group, name and version");
            }
            if (!refs.add(Json.string(component.get("bom-ref")))) {
                throw new IllegalArgumentException("duplicate bom-ref " + component.get("bom-ref"));
            }
            libraries.add(dependency);
        }
        return libraries;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
