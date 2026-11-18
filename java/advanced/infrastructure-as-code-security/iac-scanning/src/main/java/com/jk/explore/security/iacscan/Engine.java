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

    /** What each container sets that the rules read, as "container  key=value, ..." lines no wider than `width`. */
    public static List<String> settings(String text, int width) {
        List<String> out = new ArrayList<>();
        for (JsonNode doc : documents(text)) {
            JsonNode pod = podSpec(doc);
            if (pod == null) {
                continue;
            }
            for (JsonNode c : pod.path("containers")) {
                List<String> items = new ArrayList<>();
                items.add("image=" + c.path("image").asString());
                if (pod.path("hostNetwork").asBoolean(false)) {
                    items.add("hostNetwork=true");
                }
                JsonNode limits = c.path("resources").path("limits");
                if (!limits.isMissingNode()) {
                    items.add("limits=" + limits.path("cpu").asString() + "/" + limits.path("memory").asString());
                }
                for (String key : List.of("privileged", "runAsNonRoot", "readOnlyRootFilesystem", "allowPrivilegeEscalation")) {
                    JsonNode v = c.path("securityContext").path(key);
                    if (!v.isMissingNode()) {
                        items.add(key + "=" + v.asString());
                    }
                }
                for (JsonNode e : c.path("env")) {
                    items.add(e.path("name").asString() + "=" + (e.has("value") ? e.path("value").asString() : "from a Secret"));
                }
                String name = String.format("%-8s", c.path("name").asString());
                StringBuilder line = new StringBuilder(name);
                for (int i = 0; i < items.size(); i++) {
                    String item = items.get(i) + (i < items.size() - 1 ? "," : "");
                    if (line.length() > name.length() && line.length() + 1 + item.length() > width) {
                        out.add(line.toString());
                        line = new StringBuilder(" ".repeat(name.length()));
                    } else if (line.length() > name.length()) {
                        line.append(' ');
                    }
                    line.append(item);
                }
                out.add(line.toString());
            }
        }
        return out;
    }

    /** One row per container and one column per rule: x where the container breaks the rule. */
    public static List<String> matrix(String text, List<Finding> findings) {
        List<String> out = new ArrayList<>();
        StringBuilder head = new StringBuilder(" ".repeat(8));
        Rules.ALL.forEach(r -> head.append(String.format(" %-2s", r.id())));
        out.add(head.toString());
        for (JsonNode doc : documents(text)) {
            JsonNode pod = podSpec(doc);
            if (pod == null) {
                continue;
            }
            String resource = doc.path("kind").asString() + "/" + doc.path("metadata").path("name").asString();
            for (JsonNode c : pod.path("containers")) {
                String name = c.path("name").asString();
                StringBuilder row = new StringBuilder(String.format("%-8s", name));
                for (Rule r : Rules.ALL) {
                    row.append(String.format(" %-2s", findings.contains(new Finding(r.id(), resource, name)) ? "x" : "."));
                }
                long n = findings.stream().filter(f -> f.resource().equals(resource) && f.container().equals(name)).count();
                out.add(row.toString().stripTrailing() + "   " + n);
            }
        }
        return out;
    }
}
