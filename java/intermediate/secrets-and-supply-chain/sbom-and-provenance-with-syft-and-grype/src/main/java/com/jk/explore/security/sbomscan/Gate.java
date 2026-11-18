package com.jk.explore.security.sbomscan;

import java.util.List;
import java.util.TreeSet;

/**
 * The deployment gate: no critical or high advisory in the store's own libraries, except a high one that has a written exception that has not run out.
 * A critical advisory can never be excepted.
 */
final class Gate {

    /** A written exception: the advisory, the reason, and the day it ends, so that it is reviewed and cannot stay for ever. */
    record Waiver(String id, String reason, int endsOnDay) {
    }

    private Gate() {
    }

    static String check(Scan scan, List<Waiver> waivers, int today) {
        TreeSet<String> critical = new TreeSet<>();
        TreeSet<String> high = new TreeSet<>();
        TreeSet<String> excepted = new TreeSet<>();
        for (Scan.Finding f : scan.severeIn("log4j")) {
            boolean covered = f.severity().equals("High")
                    && waivers.stream().anyMatch(w -> covers(w, f, today));
            (covered ? excepted : f.severity().equals("Critical") ? critical : high).add(f.cve());
        }
        if (critical.isEmpty() && high.isEmpty()) {
            return "deployed" + (excepted.isEmpty() ? "" : ", " + excepted.size() + " high excepted");
        }
        return "refused: " + critical.size() + " critical, " + high.size() + " high";
    }

    /** A waiver covers a finding when it names it (by its own id or its CVE) and has not ended. */
    private static boolean covers(Waiver w, Scan.Finding f, int today) {
        return (w.id().equals(f.id()) || w.id().equals(f.alias())) && w.endsOnDay() >= today;
    }
}
