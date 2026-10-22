package com.jk.explore.security.iacscan;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads the manifests (several documents in one text) and applies the rules to every container. */
public final class Engine {

    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    private Engine() {
    }

    public static List<JsonNode> documents(String text) {
        List<JsonNode> out = new ArrayList<>();
        try (MappingIterator<JsonNode> it = YAML.readerFor(JsonNode.class).<JsonNode>readValues(text)) {
            while (it.hasNext()) {
                out.add(it.next());
            }
        }
        return out;
    }

    /** The pod spec of a workload: a Pod has it directly, the others under spec.template. */
    static JsonNode podSpec(JsonNode doc) {
        return switch (doc.path("kind").asString("")) {
            case "Pod" -> doc.path("spec");
            case "Deployment", "DaemonSet", "StatefulSet", "ReplicaSet", "Job" -> doc.path("spec").path("template").path("spec");
            default -> null;
        };
    }

    public static List<Finding> scan(String text, List<Rule> rules) {
        List<Finding> out = new ArrayList<>();
        for (JsonNode doc : documents(text)) {
            JsonNode pod = podSpec(doc);
            if (pod == null) {
                continue;
            }
            String resource = doc.path("kind").asString() + "/" + doc.path("metadata").path("name").asString();
            for (JsonNode c : pod.path("containers")) {
                for (Rule r : rules) {
                    if (r.breaks().test(new Rule.Container(pod, c))) {
                        out.add(new Finding(r.id(), resource, c.path("name").asString()));
                    }
                }
            }
        }
        return out;
    }

    public static List<Finding> scan(String text) {
        return scan(text, Rules.ALL);
    }
}
