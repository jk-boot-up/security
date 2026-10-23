package com.jk.explore.security.detectioncode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** The ATT&CK techniques the rules are tagged with against the techniques the labelled attacks used, and what each rule really caught. */
final class Coverage {

    /** One technique: the rules tagged with it, the attacks that used it, and how many of those some rule alerted on. */
    record Row(String technique, List<String> rules, int attacks, int detected) {

        String status() {
            if (rules.isEmpty()) {
                return "GAP no rule";
            }
            if (attacks == 0) {
                return "untested, no labelled attack";
            }
            return detected == attacks ? "covered" : "partial";
        }
    }

    private Coverage() {
    }

    /** alerted holds the ids of every event any rule alerted on. */
    static List<Row> matrix(Map<String, Rule> rules, Corpus c, Set<Integer> alerted) {
        Map<String, List<String>> tagged = new TreeMap<>();
        for (Rule r : rules.values()) {
            for (String t : r.techniques()) {
                List<String> names = tagged.computeIfAbsent(t, k -> new ArrayList<>());
                if (!names.contains(r.name()) && !r.name().equals("auth-failure")) {
                    names.add(r.name());
                }
            }
        }
        Map<String, Set<Integer>> attacked = new TreeMap<>();
        Set<Integer> caught = new TreeSet<>();
        for (Truth t : c.truth().values()) {
            if (t.attack()) {
                attacked.computeIfAbsent(t.technique(), k -> new TreeSet<>()).add(t.instance());
                if (alerted.contains(t.id())) {
                    caught.add(t.instance());
                }
            }
        }
        Set<String> all = new TreeSet<>(tagged.keySet());
        all.addAll(attacked.keySet());
        List<Row> rows = new ArrayList<>();
        for (String t : all) {
            Set<Integer> inst = attacked.getOrDefault(t, Set.of());
            rows.add(new Row(t, tagged.getOrDefault(t, List.of()), inst.size(), (int) inst.stream().filter(caught::contains).count()));
        }
        return rows;
    }
}
