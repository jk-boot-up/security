package com.jk.explore.security.detectioncode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** A Sigma rule read from its YAML: a detection (selections and a condition) or an event_count correlation over another rule. */
final class Rule {

    /** A correlation: gte or more events of the base rule from the same group within span seconds. */
    record Correlation(String base, List<String> groupBy, long spanSeconds, int gte) {
    }

    private final Map<String, Object> doc;
    private final String name;
    private final Predicate<Map<String, String>> detection;
    private final Correlation correlation;

    Rule(Map<String, Object> doc) {
        this.doc = doc;
        this.name = (String) doc.get("name");
        if (doc.containsKey("correlation")) {
            Map<String, Object> c = Sigma.map(doc.get("correlation"));
            Map<String, Object> cond = Sigma.map(c.get("condition"));
            this.correlation = new Correlation(Sigma.strings(c.get("rules")).getFirst(), Sigma.strings(c.get("group-by")),
                    Sigma.seconds(String.valueOf(c.get("timespan"))), Integer.parseInt(String.valueOf(cond.get("gte"))));
            this.detection = null;
        } else {
            this.correlation = null;
            this.detection = Sigma.compile(Sigma.map(doc.get("detection")));
        }
    }

    String name() {
        return name;
    }

    Map<String, Object> doc() {
        return doc;
    }

    Correlation correlation() {
        return correlation;
    }

    boolean matches(Map<String, String> fields) {
        return detection.test(fields);
    }

    /** The ATT&CK technique ids in the tags, such as T1505.003. */
    List<String> techniques() {
        List<String> out = new ArrayList<>();
        for (String tag : Sigma.strings(doc.get("tags"))) {
            if (Pattern.matches("attack\\.t\\d{4}(\\.\\d{3})?", tag)) {
                out.add(tag.substring("attack.".length()).toUpperCase());
            }
        }
        return out;
    }
}
