package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The tests that sit beside a rule: rules/tests/NAME.json holds cases, each a few events and whether the rule must alert on them
 * (should match) or must not (should not match). An event may have "t" (seconds from the start) and "repeat" with "every" (seconds).
 */
final class RuleTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    record Case(String name, boolean expect, List<Ev> events) {
    }

    private RuleTests() {
    }

    static Map<String, List<Case>> load(Path dir) throws IOException {
        Map<String, List<Case>> out = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path file : files) {
            JsonNode doc = JSON.readTree(Files.readString(file));
            List<Case> cases = new ArrayList<>();
            for (JsonNode c : doc.get("cases")) {
                List<Ev> events = new ArrayList<>();
                for (JsonNode e : c.get("events")) {
                    long t = e.path("t").asLong(0);
                    int repeat = e.path("repeat").asInt(1);
                    long every = e.path("every").asLong(0);
                    Map<String, String> f = new LinkedHashMap<>();
                    e.properties().forEach(kv -> {
                        if (!List.of("t", "repeat", "every").contains(kv.getKey())) {
                            f.put(kv.getKey(), kv.getValue().asString());
                        }
                    });
                    for (int i = 0; i < repeat; i++) {
                        events.add(new Ev(events.size() + 1, Corpus.START + (t + i * every) * 1000, f));
                    }
                }
                cases.add(new Case(c.get("name").asString(), c.get("match").asBoolean(), events));
            }
            out.put(doc.get("rule").asString(), cases);
        }
        return out;
    }

    static boolean passes(Case c, Rule rule, Map<String, Rule> all) {
        return !Engine.alerts(rule, all, c.events()).isEmpty() == c.expect();
    }

    /** The cases of one rule that fail against the rule as given. */
    static List<String> failures(List<Case> cases, Rule rule, Map<String, Rule> all) {
        return cases.stream().filter(c -> !passes(c, rule, all)).map(Case::name).toList();
    }
}
