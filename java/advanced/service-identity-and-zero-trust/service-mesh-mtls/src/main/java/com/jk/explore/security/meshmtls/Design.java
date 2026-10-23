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
