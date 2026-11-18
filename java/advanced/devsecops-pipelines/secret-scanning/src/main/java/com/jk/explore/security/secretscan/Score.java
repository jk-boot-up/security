package com.jk.explore.security.secretscan;

import java.util.List;

/** What a scanner got right against what was planted. */
public record Score(List<Planted> found, List<Planted> missed, List<Planted> falseAlarms, int unrelated) {

    public static Score of(List<Finding> findings) {
        List<Planted> found = Planted.realOnes().stream().filter(p -> hit(p, findings)).toList();
        List<Planted> missed = Planted.realOnes().stream().filter(p -> !found.contains(p)).toList();
        List<Planted> alarms = Planted.ALL.stream().filter(p -> !p.real() && hit(p, findings)).toList();
        long unrelated = findings.stream().filter(f -> Planted.ALL.stream().noneMatch(p -> p.in(f.text()))).count();
        return new Score(found, missed, alarms, (int) unrelated);
    }

    public String line() {
        return String.format("secrets found %d of %d, look-alikes flagged %d of %d", found.size(), Planted.realOnes().size(), falseAlarms.size(),
                Planted.ALL.size() - Planted.realOnes().size());
    }

    /** True when any finding holds the planted value. */
    static boolean hit(Planted p, List<Finding> findings) {
        return findings.stream().anyMatch(f -> p.in(f.text()));
    }

    /** What this scanner said about one planted string: found or missed for a real secret, flagged or quiet for a look-alike. */
    public String verdict(Planted p) {
        if (p.real()) {
            return found.contains(p) ? "found" : "missed";
        }
        return falseAlarms.contains(p) ? "flagged" : "quiet";
    }

    /** Recall: the real secrets found, of all real secrets. */
    public String recall() {
        return ratio(found.size(), Planted.realOnes().size());
    }

    /** Precision: the real secrets found, of every planted string flagged. */
    public String precision() {
        return ratio(found.size(), found.size() + falseAlarms.size());
    }

    private static String ratio(int a, int b) {
        return a + "/" + b + " = " + Math.round(100.0 * a / b) + "%";
    }

    static String names(List<Planted> ps) {
        return ps.isEmpty() ? "none" : String.join(", ", ps.stream().map(Planted::id).toList());
    }
}
