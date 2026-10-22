package com.jk.explore.security.iacscan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A written exception: who owns it, why, and when it ends. A finding hidden by an expired waiver comes back. */
public record Waiver(String rule, String resource, String owner, String reason, LocalDate expires) {

    boolean valid() {
        return owner != null && !owner.isBlank() && reason != null && !reason.isBlank() && expires != null;
    }

    boolean covers(Finding f, LocalDate today) {
        return valid() && rule.equals(f.rule()) && resource.equals(f.resource()) && !today.isAfter(expires);
    }

    /** The findings that remain after the waivers that are valid and not yet expired on that day. */
    public static List<Finding> apply(List<Finding> findings, List<Waiver> waivers, LocalDate today) {
        List<Finding> out = new ArrayList<>();
        for (Finding f : findings) {
            if (waivers.stream().noneMatch(w -> w.covers(f, today))) {
                out.add(f);
            }
        }
        return out;
    }

    /** The waivers that are no longer in force or are not valid, for a report. */
    public static List<Waiver> stale(List<Waiver> waivers, LocalDate today) {
        return waivers.stream().filter(w -> !w.valid() || today.isAfter(w.expires())).toList();
    }
}
