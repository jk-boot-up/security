package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** pySigma with its OpenSearch backend, installed with pip into a virtual environment under build/ the first time, and the script that converts the rules. */
final class Pysigma {

    static final String PYSIGMA = "1.5.1";
    static final String OPENSEARCH_BACKEND = "2.0.3";
    static final String ELASTICSEARCH_BACKEND = "2.1.1";

    /** The queries pySigma made for a rule, or why it could not. */
    record Conversion(List<String> queries, String error) {
    }

    private final Path dir;

    Pysigma(Path build) throws IOException, InterruptedException {
        this.dir = build.toAbsolutePath().resolve("pysigma-venv");
        Path marker = dir.resolve("installed-" + PYSIGMA + "-" + OPENSEARCH_BACKEND + "-" + ELASTICSEARCH_BACKEND);
        if (!Files.exists(marker)) {
            run(List.of("python3", "-m", "venv", dir.toString()));
            run(List.of(dir.resolve("bin/pip").toString(), "install", "--quiet", "pysigma==" + PYSIGMA, "pysigma-backend-opensearch==" + OPENSEARCH_BACKEND,
                    "pysigma-backend-elasticsearch==" + ELASTICSEARCH_BACKEND));
            Files.writeString(marker, "ok");
        }
    }

    private static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(900, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    Map<String, Conversion> convert(Path rules) throws IOException, InterruptedException {
        Path err = Files.createTempFile("pysigma", ".err");
        Process p = new ProcessBuilder(dir.resolve("bin/python").toString(), "pysigma/convert.py", rules.toString()).redirectError(err.toFile()).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("pysigma/convert.py failed: " + Files.readString(err));
        }
        Files.deleteIfExists(err);
        Map<String, Conversion> result = new LinkedHashMap<>();
        JsonNode root = JsonMapper.builder().build().readTree(out);
        root.properties().forEach(kv -> {
            List<String> queries = new ArrayList<>();
            kv.getValue().path("queries").forEach(q -> queries.add(q.asString()));
            result.put(kv.getKey(), new Conversion(queries, kv.getValue().has("error") ? kv.getValue().get("error").asString() : null));
        });
        return result;
    }
}
