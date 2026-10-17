package com.jk.explore.security.leastprivilegetf;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * An IAM policy document as the Terraform data source `aws_iam_policy_document` writes it. Only the parts that decide a request
 * are read: effect, action, not-action and resource. A Condition is refused, because this evaluator cannot honour it.
 */
record PolicyDocument(String name, List<Statement> statements) {

    record Statement(String sid, boolean allow, List<String> actions, List<String> notActions, List<String> resources) {
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    static PolicyDocument parse(String name, String json) {
        JsonNode root = MAPPER.readTree(json);
        JsonNode statements = root.get("Statement");
        if (statements == null) {
            throw new IllegalArgumentException("a policy needs a Statement");
        }
        List<Statement> parsed = new ArrayList<>();
        if (statements.isArray()) {
            for (JsonNode node : statements) {
                parsed.add(statement(node));
            }
        } else {
            parsed.add(statement(statements));
        }
        return new PolicyDocument(name, List.copyOf(parsed));
    }

    private static Statement statement(JsonNode node) {
        if (node.has("Condition")) {
            throw new IllegalArgumentException("conditions are not evaluated here, so a statement with one is refused");
        }
        String effect = node.get("Effect").asString();
        if (!effect.equals("Allow") && !effect.equals("Deny")) {
            throw new IllegalArgumentException("Effect must be Allow or Deny");
        }
        JsonNode sid = node.get("Sid");
        return new Statement(sid == null ? "" : sid.asString(), effect.equals("Allow"),
                strings(node.get("Action")), strings(node.get("NotAction")), strings(node.get("Resource")));
    }

    /** A JSON value that is either one string or a list of strings. */
    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null) {
            return values;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                values.add(item.asString());
            }
        } else {
            values.add(node.asString());
        }
        return List.copyOf(values);
    }
}
