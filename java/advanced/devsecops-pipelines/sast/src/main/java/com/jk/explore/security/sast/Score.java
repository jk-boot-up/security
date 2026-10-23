package com.jk.explore.security.sast;

import java.util.List;

/** What a rule set got right on the lines the code under test marks, for the markers whose id starts with a prefix. */
public record Score(List<Marker> found, List<Marker> missed, List<Marker> flaggedSafe, int safeTotal, int otherFindings) {

    public static Score of(List<Marker> markers, List<Finding> findings, String prefix) {
        List<Marker> mine = markers.stream().filter(m -> m.id().startsWith(prefix)).toList();
        List<String> hits = findings.stream().map(Finding::key).distinct().toList();
        List<Marker> found = mine.stream().filter(m -> m.vulnerable() && hits.contains(m.key())).toList();
        List<Marker> missed = mine.stream().filter(m -> m.vulnerable() && !hits.contains(m.key())).toList();
        List<Marker> flagged = mine.stream().filter(m -> !m.vulnerable() && hits.contains(m.key())).toList();
        int safe = (int) mine.stream().filter(m -> !m.vulnerable()).count();
        long other = findings.stream().filter(f -> markers.stream().noneMatch(m -> m.key().equals(f.key()))).count();
        return new Score(found, missed, flagged, safe, (int) other);
    }

    public int vulnerable() {
        return found.size() + missed.size();
    }

    public String line() {
        return String.format("%d of %d flaws found, %d of %d safe lines flagged", found.size(), vulnerable(), flaggedSafe.size(), safeTotal);
    }

    static String names(List<Marker> ms) {
        return ms.isEmpty() ? "none" : String.join(", ", ms.stream().map(Marker::id).toList());
    }
}
