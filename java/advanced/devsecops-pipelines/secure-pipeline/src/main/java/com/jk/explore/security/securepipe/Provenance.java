package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** An in-toto statement with a SLSA v1 provenance predicate: what was built, from what, by whom and how. */
final class Provenance {

    static final String BUILDER = "https://bank.example/securepipe/builder/v1";
    static final String BUILD_TYPE = "https://bank.example/securepipe/gradle-jar/v1";
    static final String COMMAND = "gradlew jar";

    private Provenance() {
    }

    /** What the builder saw: the jar it made and the commit, tree and dependencies it made it from. */
    record Facts(String jarName, String jarSha256, String commit, String tree, List<Sbom.Component> dependencies, String buildGradleSha256) {
    }

    static String create(Facts f) {
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("name", f.jarName());
        subject.put("digest", Map.of("sha256", f.jarSha256()));

        List<Object> inputs = new ArrayList<>();
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("uri", "git+https://git.bank.example/bank-ledger@refs/heads/main");
        Map<String, Object> sourceDigest = new LinkedHashMap<>();
        sourceDigest.put("gitCommit", f.commit());
        sourceDigest.put("gitTree", f.tree());
        source.put("digest", sourceDigest);
        inputs.add(source);
        inputs.add(Map.of("uri", "file:build.gradle", "digest", Map.of("sha256", f.buildGradleSha256())));
        for (Sbom.Component c : f.dependencies()) {
            inputs.add(Map.of("uri", c.purl()));
        }

        Map<String, Object> external = new LinkedHashMap<>();
        external.put("command", COMMAND);
        external.put("repository", "bank-ledger");
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("buildType", BUILD_TYPE);
        definition.put("externalParameters", external);
        definition.put("resolvedDependencies", inputs);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("builder", Map.of("id", BUILDER));
        Map<String, Object> predicate = new LinkedHashMap<>();
        predicate.put("buildDefinition", definition);
        predicate.put("runDetails", details);

        Map<String, Object> statement = new LinkedHashMap<>();
        statement.put("_type", "https://in-toto.io/Statement/v1");
        statement.put("subject", List.of(subject));
        statement.put("predicateType", "https://slsa.dev/provenance/v1");
        statement.put("predicate", predicate);
        return new JsonMapper().writeValueAsString(statement);
    }

    static JsonNode read(Path file) throws IOException {
        return new JsonMapper().readTree(Files.readString(file));
    }

    static String subjectDigest(JsonNode statement) {
        return statement.path("subject").path(0).path("digest").path("sha256").asString();
    }

    static String builderId(JsonNode statement) {
        return statement.path("predicate").path("runDetails").path("builder").path("id").asString();
    }

    static String sourceCommit(JsonNode statement) {
        return statement.path("predicate").path("buildDefinition").path("resolvedDependencies").path(0).path("digest").path("gitCommit").asString();
    }

    static String sourceTree(JsonNode statement) {
        return statement.path("predicate").path("buildDefinition").path("resolvedDependencies").path(0).path("digest").path("gitTree").asString();
    }

    static String command(JsonNode statement) {
        return statement.path("predicate").path("buildDefinition").path("externalParameters").path("command").asString();
    }

    static int inputs(JsonNode statement) {
        return statement.path("predicate").path("buildDefinition").path("resolvedDependencies").size();
    }
}
