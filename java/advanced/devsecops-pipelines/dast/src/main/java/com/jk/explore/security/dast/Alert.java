package com.jk.explore.security.dast;

import java.util.List;

/**
 * One kind of alert in a ZAP report: its name, its risk and confidence (3 high, 2 medium, 1 low, 0 informational), and where it
 * was raised: each hit is a path, the parameter that was attacked and the attack string, if any.
 */
public record Alert(String name, int risk, int confidence, List<Hit> hits) {

    /** One place an alert was raised. */
    public record Hit(String path, String param, String attack) {
    }

    static final List<String> LEVELS = List.of("informational", "low", "medium", "high");

    List<String> paths() {
        return hits.stream().map(Hit::path).distinct().sorted().toList();
    }

    boolean on(String path) {
        return paths().contains(path);
    }

    String riskName() {
        return LEVELS.get(risk);
    }

    String confidenceName() {
        return LEVELS.get(confidence);
    }
}
