package com.jk.explore.security.meshmtls;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Which workload may call which, as the design says, and a measured matrix compared with it. */
public record Design(Set<String> allowed) {

    /** One line of the design: from, to. */
    public static String pair(Service from, Service to) {
        return from.id() + " -> " + to.id();
    }

    /** What a measurement showed against the design. */
    public record Diff(int answered, int total, List<String> unwanted, List<String> missing) {

        public boolean matches() {
            return unwanted.isEmpty() && missing.isEmpty();
        }
    }

    /** The measurement as a table: a row per caller, a column per service; yes when the call was answered, - for a service calling itself. */
    public static List<String> table(List<Service> callers, List<Service> targets, Set<String> answered) {
        List<String> rows = new ArrayList<>();
        StringBuilder head = new StringBuilder(String.format("%-16s", "caller"));
        for (Service to : targets) {
            head.append(String.format("%-11s", to.name()));
        }
        rows.add(head.toString().stripTrailing());
        for (Service from : callers) {
            StringBuilder row = new StringBuilder(String.format("%-16s", from.id()));
            for (Service to : targets) {
                row.append(String.format("%-11s", from.equals(to) ? "-" : answered.contains(pair(from, to)) ? "yes" : "no"));
            }
            rows.add(row.toString().stripTrailing());
        }
        return rows;
    }

    public Diff compare(List<Service> callers, List<Service> targets, Set<String> answered) {
        List<String> unwanted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int total = 0;
        for (Service from : callers) {
            for (Service to : targets) {
                if (from.equals(to)) {
                    continue;
                }
                total++;
                String pair = pair(from, to);
                boolean open = answered.contains(pair);
                if (open && !allowed.contains(pair)) {
                    unwanted.add(pair);
                } else if (!open && allowed.contains(pair)) {
                    missing.add(pair);
                }
            }
        }
        return new Diff(answered.size(), total, unwanted, missing);
    }
}
