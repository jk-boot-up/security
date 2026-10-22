package com.jk.explore.security.sbomscan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** A CycloneDX bill of materials written by syft, read back. */
record Sbom(String json, Map<String, Object> document, List<Map<String, Object>> components) {

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** Catalogues an image with syft and returns the CycloneDX document. */
    static Sbom of(String image) throws IOException, InterruptedException {
        return parse(Lab.run(List.of("syft", image, "-o", "cyclonedx-json", "-q")));
    }

    @SuppressWarnings("unchecked")
    static Sbom parse(String json) {
        Map<String, Object> document = JSON.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return new Sbom(json, document, (List<Map<String, Object>>) document.get("components"));
    }

    /** The package URLs of the libraries, which identify a package by ecosystem, name and version. */
    TreeSet<String> libraries() {
        TreeSet<String> out = new TreeSet<>();
        for (Map<String, Object> c : components) {
            if ("library".equals(c.get("type")) && c.get("purl") != null) {
                out.add(String.valueOf(c.get("purl")));
            }
        }
        return out;
    }

    List<String> withName(String text) {
        List<String> out = new ArrayList<>();
        for (String purl : libraries()) {
            if (purl.contains(text)) {
                out.add(purl);
            }
        }
        return out;
    }

    String specVersion() {
        return String.valueOf(document.get("specVersion"));
    }

    @SuppressWarnings("unchecked")
    String subject() {
        Map<String, Object> component = (Map<String, Object>) ((Map<String, Object>) document.get("metadata")).get("component");
        return component.get("name") + ":" + component.get("version");
    }

    boolean mentions(String text) {
        return json.contains(text);
    }
}
