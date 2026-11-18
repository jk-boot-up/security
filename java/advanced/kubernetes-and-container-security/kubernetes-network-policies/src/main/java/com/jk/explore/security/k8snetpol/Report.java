package com.jk.explore.security.k8snetpol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Turns pods, policies and measurements into the demo's lines. No tools: the tests feed it recorded measurements. */
final class Report {

    static final String OPEN = "o";
    static final String CLOSED = ".";
    /** Longer lists are counted, not listed: the matrix above shows them. */
    static final int LISTED = 4;

    private Report() {
    }

    static List<String> pods(List<Workload> pods) {
        return pods.stream().map(w -> String.format("%-12s app=%-6s %s", w.id(), w.app(), w.role())).toList();
    }

    /** The design's pairs, in the order of the pods. */
    static List<String> design(Intent intent, List<Workload> pods) {
        List<String> lines = new ArrayList<>();
        for (Workload from : pods) {
            for (Workload to : pods) {
                if (intent.allowed().contains(Intent.pair(from, to))) {
                    lines.add(Intent.pair(from, to));
                }
            }
        }
        return lines;
    }

    static List<String> policies(List<Policy> policies) {
        return policies.stream().map(p -> String.format("%-24s %s", p.name(), p.summary())).toList();
    }

    /** Who reaches whom: one row for each caller, one column for each callee, o for answered and . for blocked. */
    static List<String> matrix(List<Workload> pods, Set<String> reachable) {
        List<String> lines = new ArrayList<>();
        StringBuilder head = new StringBuilder(String.format("%-10s", "from\\to"));
        for (Workload to : pods) {
            head.append(String.format("%-7s", to.name()));
        }
        lines.add(head.toString().stripTrailing());
        for (Workload from : pods) {
            StringBuilder row = new StringBuilder(String.format("%-10s", from.name()));
            for (Workload to : pods) {
                String mark = from.equals(to) ? "-" : reachable.contains(Intent.pair(from, to)) ? OPEN : CLOSED;
                row.append(String.format("%-7s", mark));
            }
            lines.add(row.toString().stripTrailing());
        }
        return lines;
    }

    /** n pods make n x (n - 1) ordered pairs: each pod can call each other pod. */
    static String pairCount(int pods) {
        return String.format("%d pods   %d x %d = %d pairs", pods, pods, pods - 1, pods * (pods - 1));
    }

    /** The measurement against the design: reachable = wanted + unwanted, and what the design lists but is closed. */
    static List<String> against(Intent.Diff d) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("reachable   %d of %d = %d wanted + %d unwanted",
                d.reachable(), d.total(), d.wanted(), d.unwanted().size()));
        listed(lines, "unwanted", d.unwanted());
        listed(lines, "missing", d.missing());
        return lines;
    }

    private static void listed(List<String> lines, String key, List<String> pairs) {
        if (pairs.isEmpty()) {
            lines.add(String.format("%-12s0", key));
            return;
        }
        if (pairs.size() > LISTED) {
            lines.add(String.format("%-12s%d, every o that is not in the design", key, pairs.size()));
            return;
        }
        for (int i = 0; i < pairs.size(); i++) {
            lines.add(String.format("%-12s%s", i == 0 ? key + " " + pairs.size() : "", pairs.get(i)));
        }
    }

    /** Curl's exit code in words. */
    static String exit(int code) {
        return switch (code) {
            case 0 -> "answered";
            case 6 -> "name not found";
            case 28 -> "timed out";
            default -> "failed with " + code;
        };
    }
}
