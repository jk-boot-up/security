package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Turns a scanner report into a decision: how many of each severity, what order to work in, and the version to move each library to. */
final class Triage {

    private Triage() {
    }

    static Map<String, Integer> countBySeverity(List<Vulnerability> vulnerabilities) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String severity : List.of("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            counts.put(severity, 0);
        }
        for (Vulnerability v : vulnerabilities) {
            counts.merge(v.severity(), 1, Integer::sum);
        }
        return counts;
    }

    /** Most serious first; for equal severity, by library and then advisory id, so the order is the same every time. */
    static List<Vulnerability> ordered(List<Vulnerability> vulnerabilities) {
        List<Vulnerability> sorted = new ArrayList<>(vulnerabilities);
        sorted.sort(Comparator.comparingInt((Vulnerability v) -> -Vulnerability.rank(v.severity()))
                .thenComparing(Vulnerability::packageName).thenComparing(Vulnerability::id));
        return sorted;
    }

    /** For each library, the version that fixes every problem that has a fix: the highest of the nearest fixes. Libraries with no fix map to empty. */
    static Map<String, Optional<Version>> upgradeTargets(List<Vulnerability> vulnerabilities) {
        Map<String, Optional<Version>> targets = new TreeMap<>();
        for (Vulnerability v : vulnerabilities) {
            Optional<Version> fix = v.nearestFix();
            Optional<Version> current = targets.getOrDefault(v.packageName(), Optional.empty());
            if (fix.isPresent() && (current.isEmpty() || current.get().isOlderThan(fix.get()))) {
                targets.put(v.packageName(), fix);
            } else {
                targets.putIfAbsent(v.packageName(), current);
            }
        }
        return targets;
    }
}
