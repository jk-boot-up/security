package com.jk.explore.security.securitylogging;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes lines older than the retention period. The first kept line's "prev" becomes the new anchor, so the remaining chain still
 * checks: the anchor is written down when old lines go, in the same place the head hash is published.
 */
final class Retention {

    record Pruned(List<String> kept, int removed, String anchor) {
    }

    private Retention() {
    }

    static Pruned prune(List<String> lines, Instant now, Duration keep) {
        Instant cutoff = now.minus(keep);
        List<String> kept = new ArrayList<>();
        String anchor = null;
        int removed = 0;
        for (String line : lines) {
            if (Instant.parse(Json.parse(line).get("time")).isBefore(cutoff)) {
                removed++;
            } else {
                if (anchor == null) {
                    anchor = Json.parse(line).get("prev");
                }
                kept.add(line);
            }
        }
        return new Pruned(kept, removed, anchor == null ? SecurityLog.GENESIS : anchor);
    }
}
