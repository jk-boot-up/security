package com.jk.explore.security.detectioncode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A teaching class: each selection of a rule decided on its own for one event, so the steps of a condition can be shown.
 * A test checks that the condition worked out from these answers is the rule's own result.
 */
final class ConditionSteps {

    private ConditionSteps() {
    }

    /** Every selection of the rule, by name, and whether the event matches it. */
    static Map<String, Boolean> selections(Rule rule, Map<String, String> event) {
        Map<String, Object> detection = Sigma.map(rule.doc().get("detection"));
        Map<String, Boolean> out = new LinkedHashMap<>();
        detection.forEach((name, body) -> {
            if (!name.equals("condition")) {
                // a detection of one selection, whose condition is that selection
                out.put(name, Sigma.compile(Map.of(name, body, "condition", name)).test(event));
            }
        });
        return out;
    }

    /** The rule's selections as printed lines: "name: Field|modifier" and the values below it. */
    static List<String> describe(Rule rule) {
        Map<String, Object> detection = Sigma.map(rule.doc().get("detection"));
        List<String> out = new java.util.ArrayList<>();
        detection.forEach((name, body) -> {
            if (name.equals("condition")) {
                return;
            }
            Sigma.map(body).forEach((field, values) -> {
                out.add(name + ": " + field);
                out.add("  " + String.join("  ", Sigma.strings(values)));
            });
        });
        return out;
    }
}
