package com.jk.explore.security.iammodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A policy document in the usual JSON form: statements with an effect, actions, resources, a principal (for resource policies) and conditions. */
public record Policy(String name, List<Statement> statements) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** One statement. {@code notAction} and {@code notResource} are the "everything except" forms. */
    public record Statement(boolean allow, List<String> actions, boolean notAction, List<String> resources, boolean notResource, List<String> principals, Map<String, Map<String, List<String>>> conditions) {
    }

    public static Policy parse(String name, String json) {
        JsonNode root = JSON.readTree(json);
        JsonNode statements = root.get("Statement");
        List<Statement> out = new ArrayList<>();
        for (JsonNode s : statements.isArray() ? statements : List.of(statements)) {
            boolean notAction = s.has("NotAction");
            boolean notResource = s.has("NotResource");
            Map<String, Map<String, List<String>>> conditions = new java.util.LinkedHashMap<>();
            if (s.has("Condition")) {
                s.get("Condition").properties().forEach(op -> {
                    Map<String, List<String>> keys = new java.util.LinkedHashMap<>();
                    op.getValue().properties().forEach(k -> keys.put(k.getKey(), strings(k.getValue())));
                    conditions.put(op.getKey(), keys);
                });
            }
            List<String> principals = new ArrayList<>();
            if (s.has("Principal")) {
                JsonNode p = s.get("Principal");
                if (p.isString()) {
                    principals.add(p.asString());
                } else {
                    p.properties().forEach(e -> principals.addAll(strings(e.getValue())));
                }
            }
            out.add(new Statement(s.get("Effect").asString().equals("Allow"), strings(s.has("Action") ? s.get("Action") : s.get("NotAction")), notAction,
                    strings(s.has("Resource") ? s.get("Resource") : s.has("NotResource") ? s.get("NotResource") : JSON.createArrayNode().add("*")), notResource, principals, conditions));
        }
        return new Policy(name, out);
    }

    private static List<String> strings(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n.isArray()) {
            n.forEach(x -> out.add(x.asString()));
        } else {
            out.add(n.asString());
        }
        return out;
    }

    /** A policy with one statement, written inline: allow or deny, one action pattern and one resource pattern. */
    public static Policy of(String name, boolean allow, String action, String resource) {
        return new Policy(name, List.of(new Statement(allow, List.of(action), false, List.of(resource), false, List.of(), Map.of())));
    }
}
