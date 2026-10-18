package com.jk.explore.security.provenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A CycloneDX 1.6 bill of materials in JSON. It says what is inside a build: the application, its hash, and each component with its
 * package URL and SHA-256. It does not say who built it, on which machine, or from which source repository and commit.
 */
final class Sbom {

    static final String SERIAL = "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79";

    private Sbom() {
    }

    static String write(String appName, String appVersion, byte[] artifact, List<Component> components) {
        List<Object> list = new ArrayList<>();
        for (Component c : components) {
            list.add(Json.object(
                    "type", "library",
                    "bom-ref", c.purl(),
                    "group", c.group(),
                    "name", c.name(),
                    "version", c.version(),
                    "purl", c.purl(),
                    "hashes", List.of(Json.object("alg", "SHA-256", "content", c.sha256()))));
        }
        Map<String, Object> bom = Json.object(
                "bomFormat", "CycloneDX",
                "specVersion", "1.6",
                "serialNumber", SERIAL,
                "version", 1L,
                "metadata", Json.object(
                        "timestamp", "2026-10-01T09:05:00Z",
                        "component", Json.object(
                                "type", "application",
                                "name", appName,
                                "version", appVersion,
                                "hashes", List.of(Json.object("alg", "SHA-256", "content", Digests.hex(Digests.sha256(artifact)))))),
                "components", list);
        return Json.write(bom);
    }

    /** Whether this SBOM is the one for this artifact: its application hash is the artifact's SHA-256. */
    static boolean describes(String sbom, byte[] artifact) throws Json.BadJsonException {
        Object root = Json.read(sbom);
        Object hashes = Json.at(root, "metadata", "component", "hashes");
        if (!(hashes instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        String content = Json.text(list.get(0), "content");
        return content != null && Digests.same(Digests.unhex(content), Digests.sha256(artifact));
    }

    /** The package URLs listed in the SBOM, in order. */
    static List<String> purls(String sbom) throws Json.BadJsonException {
        List<String> out = new ArrayList<>();
        if (Json.at(Json.read(sbom), "components") instanceof List<?> list) {
            for (Object c : list) {
                out.add(Json.text(c, "purl"));
            }
        }
        return out;
    }

    /** Components whose jar on disk no longer matches the hash the SBOM recorded. */
    static List<String> changedComponents(String sbom, List<Component> onDisk) throws Json.BadJsonException {
        List<String> changed = new ArrayList<>();
        Object components = Json.at(Json.read(sbom), "components");
        if (!(components instanceof List<?> list)) {
            return changed;
        }
        for (Component c : onDisk) {
            for (Object entry : list) {
                if (c.purl().equals(Json.text(entry, "purl"))
                        && Json.at(entry, "hashes") instanceof List<?> hashes && !hashes.isEmpty()
                        && !c.sha256().equals(Json.text(hashes.get(0), "content"))) {
                    changed.add(c.purl());
                }
            }
        }
        return changed;
    }
}
