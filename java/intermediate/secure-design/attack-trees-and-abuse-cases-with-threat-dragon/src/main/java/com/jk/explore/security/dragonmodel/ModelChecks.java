package com.jk.explore.security.dragonmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Checks of the model file itself, that the editor's schema does not make: the threats are complete and consistent, and the flags, numbers and links agree with each other. */
public final class ModelChecks {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> STATUS = Set.of("Open", "Mitigated", "NA");
    private static final Set<String> SEVERITY = Set.of("High", "Medium", "Low");
    private static final Set<String> STRIDE = Set.of("Spoofing", "Tampering", "Repudiation", "Information disclosure", "Denial of service", "Elevation of privilege");

    private ModelChecks() {
    }

    /** One broken rule and where. */
    public record Problem(String rule, String where) {
    }

    public static List<Problem> check(String modelJson) {
        JsonNode model = JSON.readTree(modelJson);
        List<Problem> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Integer> numbers = new HashSet<>();
        int highest = 0;
        JsonNode diagram = model.get("detail").get("diagrams").get(0);
        for (JsonNode cell : diagram.get("cells")) {
            ids.add(cell.get("id").asString());
        }
        Set<String> covered = new HashSet<>();
        for (JsonNode cell : diagram.get("cells")) {
            JsonNode data = cell.get("data");
            String name = data.get("name").asString();
            boolean anyOpen = false;
            if (data.has("threats")) {
                for (JsonNode t : data.get("threats")) {
                    String where = name + " / " + t.get("title").asString().split(":")[0];
                    for (String field : List.of("title", "status", "severity", "type", "description", "number")) {
                        if (!t.has(field) || t.get(field).asString().isBlank()) {
                            problems.add(new Problem("complete-threat", where + " lacks " + field));
                        }
                    }
                    if (!STATUS.contains(t.get("status").asString())) {
                        problems.add(new Problem("known-status", where));
                    }
                    if (!SEVERITY.contains(t.get("severity").asString())) {
                        problems.add(new Problem("known-severity", where));
                    }
                    if (!STRIDE.contains(t.get("type").asString())) {
                        problems.add(new Problem("stride-type", where));
                    }
                    if (t.get("status").asString().equals("Mitigated") && t.get("mitigation").asString().isBlank()) {
                        problems.add(new Problem("mitigated-names-its-defence", where));
                    }
                    if (t.get("status").asString().equals("Open")) {
                        anyOpen = true;
                    }
                    int number = t.get("number").asInt();
                    highest = Math.max(highest, number);
                    if (!numbers.add(number)) {
                        problems.add(new Problem("unique-numbers", where));
                    }
                    covered.add(t.get("title").asString().split(":")[0]);
                }
            }
            if (data.has("hasOpenThreats") && data.get("hasOpenThreats").asBoolean() != anyOpen) {
                problems.add(new Problem("open-flag-matches-threats", name));
            }
            if (cell.get("shape").asString().equals("flow")) {
                for (String end : List.of("source", "target")) {
                    if (!ids.contains(cell.get(end).get("cell").asString())) {
                        problems.add(new Problem("flow-ends-exist", name + " " + end));
                    }
                }
            }
        }
        if (model.get("detail").get("threatTop").asInt() != highest) {
            problems.add(new Problem("threat-top-is-highest-number", "threatTop " + model.get("detail").get("threatTop").asInt() + " but highest " + highest));
        }
        for (AbuseCase c : AbuseCase.all()) {
            if (!covered.contains(c.id())) {
                problems.add(new Problem("every-abuse-case-has-a-threat", c.id()));
            }
        }
        return problems;
    }

    /** Counts the threats with this status. */
    public static long count(String modelJson, String status) {
        long n = 0;
        for (JsonNode cell : JSON.readTree(modelJson).get("detail").get("diagrams").get(0).get("cells")) {
            if (cell.get("data").has("threats")) {
                for (JsonNode t : cell.get("data").get("threats")) {
                    if (t.get("status").asString().equals(status)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** The threat titles' ids with their status, in the order of the cells. */
    public static List<String> statuses(String modelJson) {
        List<String> out = new ArrayList<>();
        for (JsonNode cell : JSON.readTree(modelJson).get("detail").get("diagrams").get(0).get("cells")) {
            if (cell.get("data").has("threats")) {
                for (JsonNode t : cell.get("data").get("threats")) {
                    out.add(t.get("title").asString().split(":")[0] + " " + t.get("status").asString());
                }
            }
        }
        out.sort(null);
        return out;
    }
}
