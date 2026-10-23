package com.jk.explore.security.secretscan;

import java.util.List;

/** What a scanner got right against what was planted. */
public record Score(List<Planted> found, List<Planted> missed, List<Planted> falseAlarms, int unrelated) {

    public static Score of(List<Finding> findings) {
        List<Planted> found = Planted.realOnes().stream().filter(p -> findings.stream().anyMatch(f -> p.in(f.text()))).toList();
        List<Planted> missed = Planted.realOnes().stream().filter(p -> !found.contains(p)).toList();
        List<Planted> alarms = Planted.ALL.stream().filter(p -> !p.real()).filter(p -> findings.stream().anyMatch(f -> p.in(f.text()))).toList();
        long unrelated = findings.stream().filter(f -> Planted.ALL.stream().noneMatch(p -> p.in(f.text()))).count();
        return new Score(found, missed, alarms, (int) unrelated);
    }

    public String line() {
        return String.format("%d of %d real secrets found, %d look-alikes flagged, %d other alarms", found.size(), Planted.realOnes().size(), falseAlarms.size(), unrelated);
    }

    static String names(List<Planted> ps) {
        return ps.isEmpty() ? "none" : String.join(", ", ps.stream().map(Planted::id).toList());
    }
}
