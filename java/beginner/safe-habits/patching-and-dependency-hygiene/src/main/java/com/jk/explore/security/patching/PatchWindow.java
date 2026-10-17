package com.jk.explore.security.patching;

/** How long a fix may take, by severity, and how a finding stands against that deadline. Time is counted in whole days on a simulated clock. */
final class PatchWindow {

    record Status(String text, boolean withinWindow) {
    }

    private PatchWindow() {
    }

    static int slaDays(String severity) {
        return switch (severity) {
            case "Critical" -> 7;
            case "High" -> 30;
            case "Medium" -> 90;
            case "Low" -> 180;
            default -> 365;
        };
    }

    /** Where a finding stands on {@code today}, given the day it was published and the day it was patched, if it was. */
    static Status evaluate(String severity, int published, Integer patched, int today) {
        int sla = slaDays(severity);
        if (patched != null) {
            int took = patched - published;
            return took <= sla
                    ? new Status("patched in " + took + " days, within the SLA of " + sla, true)
                    : new Status("patched in " + took + " days, LATE: the SLA was " + sla, false);
        }
        int open = today - published;
        return open <= sla
                ? new Status("open for " + open + " days, " + (sla - open) + " days left in the SLA of " + sla, true)
                : new Status("open for " + open + " days, BREACHED: the SLA of " + sla + " ended " + (open - sla) + " days ago", false);
    }
}
