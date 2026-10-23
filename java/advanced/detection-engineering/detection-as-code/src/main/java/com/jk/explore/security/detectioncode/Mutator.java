package com.jk.explore.security.detectioncode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Small changes to a rule, each a loosening (more alerts) or a tightening (fewer): drop a filter, drop a field from a selection, drop one value
 * of a list, change the first "and" to "or" or "1 of" to "all of", move a correlation threshold or window. A good set of tests fails on every one.
 */
final class Mutator {

    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    /** direction is "loosen" or "tighten". */
    record Mutant(String ruleName, String direction, String what, Rule rule) {

        String label() {
            return ruleName + ": " + direction + ", " + what;
        }
    }

    private Mutator() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copy(Rule r) {
        return YAML.convertValue(r.doc(), Map.class);
    }

    /** The rule as YAML text. */
    static String dump(Rule rule) {
        return YAML.writeValueAsString(rule.doc());
    }

    static List<Mutant> of(Rule rule) {
        List<Mutant> out = new ArrayList<>();
        if (rule.correlation() != null) {
            Rule.Correlation c = rule.correlation();
            out.add(correlation(rule, "loosen", "threshold " + c.gte() + " to 2", d -> Sigma.map(Sigma.map(d.get("correlation")).get("condition")).put("gte", 2)));
            out.add(correlation(rule, "tighten", "threshold " + c.gte() + " to " + c.gte() * 3, d -> Sigma.map(Sigma.map(d.get("correlation")).get("condition")).put("gte", c.gte() * 3)));
            out.add(correlation(rule, "loosen", "window 5m to 1h", d -> Sigma.map(d.get("correlation")).put("timespan", "1h")));
            out.add(correlation(rule, "tighten", "window 5m to 1m", d -> Sigma.map(d.get("correlation")).put("timespan", "1m")));
            return out;
        }
        Map<String, Object> detection = Sigma.map(rule.doc().get("detection"));
        String condition = String.valueOf(detection.get("condition"));
        for (String sel : detection.keySet()) {
            if (sel.equals("condition")) {
                continue;
            }
            if (sel.startsWith("filter")) {
                out.add(detection(rule, "loosen", "drop " + sel, d -> Sigma.map(d.get("detection")).put("condition", condition.replace("and not " + sel, "").trim())));
                continue;
            }
            if (!(detection.get(sel) instanceof Map<?, ?> fields)) {
                continue;
            }
            for (Object field : fields.keySet()) {
                String f = String.valueOf(field);
                if (fields.size() > 1) {
                    out.add(detection(rule, "loosen", "drop the field " + f.split("\\|")[0] + " from " + sel, d -> Sigma.map(Sigma.map(d.get("detection")).get(sel)).remove(f)));
                }
                List<String> values = Sigma.strings(fields.get(field));
                if (values.size() > 1 && fields.get(field) instanceof List<?>) {
                    for (String v : values) {
                        out.add(detection(rule, "tighten", "drop the value " + v + " from " + sel, d -> {
                            List<String> left = new ArrayList<>(Sigma.strings(Sigma.map(Sigma.map(d.get("detection")).get(sel)).get(f)));
                            left.remove(v);
                            Sigma.map(Sigma.map(d.get("detection")).get(sel)).put(f, left);
                        }));
                    }
                }
            }
        }
        if (condition.contains(" and ")) {
            out.add(detection(rule, "loosen", "first 'and' to 'or'", d -> Sigma.map(d.get("detection")).put("condition", condition.replaceFirst(" and ", " or "))));
        }
        if (condition.contains("1 of ")) {
            out.add(detection(rule, "tighten", "'1 of' to 'all of'", d -> Sigma.map(d.get("detection")).put("condition", condition.replace("1 of ", "all of "))));
        }
        return out;
    }

    private static Mutant detection(Rule rule, String direction, String what, java.util.function.Consumer<Map<String, Object>> change) {
        Map<String, Object> d = copy(rule);
        change.accept(d);
        return new Mutant(rule.name(), direction, what, new Rule(d));
    }

    private static Mutant correlation(Rule rule, String direction, String what, java.util.function.Consumer<Map<String, Object>> change) {
        return detection(rule, direction, what, change);
    }

    /** The rules with one of them replaced. */
    static Map<String, Rule> with(Map<String, Rule> rules, Rule replacement) {
        Map<String, Rule> out = new LinkedHashMap<>(rules);
        out.put(replacement.name(), replacement);
        return out;
    }
}
