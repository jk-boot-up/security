package com.jk.explore.security.patching;

import java.util.Map;

/** The base score of the Common Vulnerability Scoring System, version 3.1, worked out from a vector such as AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H. */
final class Cvss31 {

    private static final Map<String, Double> ATTACK_VECTOR = Map.of("N", 0.85, "A", 0.62, "L", 0.55, "P", 0.2);
    private static final Map<String, Double> COMPLEXITY = Map.of("L", 0.77, "H", 0.44);
    private static final Map<String, Double> USER_INTERACTION = Map.of("N", 0.85, "R", 0.62);
    private static final Map<String, Double> IMPACT = Map.of("H", 0.56, "L", 0.22, "N", 0.0);

    private Cvss31() {
    }

    static double base(String vector) {
        String body = vector.startsWith("CVSS:3.1/") ? vector.substring("CVSS:3.1/".length()) : vector;
        Map<String, String> metrics = new java.util.HashMap<>();
        for (String part : body.split("/")) {
            String[] pair = part.split(":");
            if (pair.length != 2 || metrics.put(pair[0], pair[1]) != null) {
                throw new IllegalArgumentException("bad CVSS vector: " + vector);
            }
        }
        if (metrics.size() != 8) {
            throw new IllegalArgumentException("a base vector needs exactly eight metrics: " + vector);
        }
        String scope = metrics.get("S");
        if (!"U".equals(scope) && !"C".equals(scope)) {
            throw new IllegalArgumentException("bad CVSS vector: " + vector);
        }
        boolean changed = scope.equals("C");
        double attack = lookup(metrics, "AV", ATTACK_VECTOR, vector);
        double complexity = lookup(metrics, "AC", COMPLEXITY, vector);
        double privileges = switch (metrics.get("PR")) {
            case "N" -> 0.85;
            case "L" -> changed ? 0.68 : 0.62;
            case "H" -> changed ? 0.5 : 0.27;
            default -> throw new IllegalArgumentException("bad CVSS vector: " + vector);
        };
        double interaction = lookup(metrics, "UI", USER_INTERACTION, vector);
        double confidentiality = lookup(metrics, "C", IMPACT, vector);
        double integrity = lookup(metrics, "I", IMPACT, vector);
        double availability = lookup(metrics, "A", IMPACT, vector);

        double iss = 1 - (1 - confidentiality) * (1 - integrity) * (1 - availability);
        double impact = changed ? 7.52 * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15) : 6.42 * iss;
        double exploitability = 8.22 * attack * complexity * privileges * interaction;
        if (impact <= 0) {
            return 0.0;
        }
        double sum = changed ? 1.08 * (impact + exploitability) : impact + exploitability;
        return roundUp(Math.min(sum, 10));
    }

    private static double lookup(Map<String, String> metrics, String name, Map<String, Double> weights, String vector) {
        Double weight = weights.get(metrics.get(name));
        if (weight == null) {
            throw new IllegalArgumentException("bad CVSS vector: " + vector);
        }
        return weight;
    }

    /** The smallest number with one decimal place that is not below the input, as the specification defines it. */
    static double roundUp(double input) {
        long scaled = Math.round(input * 100000);
        if (scaled % 10000 == 0) {
            return scaled / 100000.0;
        }
        return (Math.floor(scaled / 10000.0) + 1) / 10.0;
    }

    static String severity(double score) {
        if (score == 0.0) {
            return "None";
        }
        if (score < 4.0) {
            return "Low";
        }
        if (score < 7.0) {
            return "Medium";
        }
        return score < 9.0 ? "High" : "Critical";
    }
}
