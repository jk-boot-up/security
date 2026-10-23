package com.jk.explore.security.k8snetpol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Which pod may reach which pod, as the design says, and a measured matrix compared with it. */
public record Intent(Set<String> allowed) {

    /** One line of the design: from, to. */
    public static String pair(Workload from, Workload to) {
        return from.id() + " -> " + to.id();
    }

    /** What a measurement showed against the design. */
    public record Diff(int reachable, int total, List<String> unwanted, List<String> missing) {

        public boolean matches() {
            return unwanted.isEmpty() && missing.isEmpty();
        }
    }

    public Diff compare(List<Workload> pods, Set<String> reachable) {
        List<String> unwanted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int total = 0;
        for (Workload from : pods) {
            for (Workload to : pods) {
                if (from.equals(to)) {
                    continue;
                }
                total++;
                String pair = pair(from, to);
                boolean open = reachable.contains(pair);
                if (open && !allowed.contains(pair)) {
                    unwanted.add(pair);
                } else if (!open && allowed.contains(pair)) {
                    missing.add(pair);
                }
            }
        }
        return new Diff(reachable.size(), total, unwanted, missing);
    }
}
