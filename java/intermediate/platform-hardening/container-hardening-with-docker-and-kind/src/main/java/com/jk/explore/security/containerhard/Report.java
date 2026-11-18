package com.jk.explore.security.containerhard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** How the demo prints what the probes and the cluster returned: labelled sections, at most {@link #WIDTH} characters a line. No tools are run here. */
final class Report {

    static final int WIDTH = 66;

    private final StringBuilder out = new StringBuilder();

    @Override
    public String toString() {
        return out.toString();
    }

    void line(String s) {
        out.append(s).append('\n');
    }

    /** A comma-separated list after a prefix, wrapped under the first item. */
    void wrap(String prefix, List<String> items) {
        StringBuilder cur = new StringBuilder(prefix);
        String indent = " ".repeat(prefix.length());
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i) + (i < items.size() - 1 ? "," : "");
            if (cur.length() > prefix.length() && cur.length() + 1 + item.length() > WIDTH) {
                line(cur.toString());
                cur = new StringBuilder(indent);
            } else if (cur.length() > prefix.length()) {
                cur.append(' ');
            }
            cur.append(item);
        }
        line(cur.toString());
    }

    /** {@code docker run} with one option (and its value) per line. */
    void command(List<String> options, String image) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String o = options.get(i);
            boolean takesValue = o.startsWith("-") && !o.contains("=") && !List.of("--privileged", "--read-only").contains(o)
                    && i + 1 < options.size() && !options.get(i + 1).startsWith("-");
            args.add(takesValue ? o + " " + options.get(++i) : o);
        }
        args.add(image);
        line("    docker run \\");
        for (int i = 0; i < args.size(); i++) {
            line("      " + args.get(i) + (i < args.size() - 1 ? " \\" : ""));
        }
    }

    /** Every attempt, grouped by what it reaches, with what the probe saw. */
    void attempts(Map<String, Boolean> r) {
        for (String reach : List.of(Probes.CONTAINER, Probes.KERNEL, Probes.HOST, Probes.RESOURCES)) {
            line("    " + reach + ":");
            Probes.REACH.forEach((name, where) -> {
                if (where.equals(reach)) {
                    line(String.format("      %-19s %-8s %s", name, r.get(name) ? "POSSIBLE" : "blocked", Probes.reason(name, r.get(name))));
                }
            });
        }
    }

    /** The possible attempts counted by what they reach, then added up. */
    void count(Map<String, Boolean> r) {
        List<String> parts = new ArrayList<>();
        for (String reach : List.of(Probes.CONTAINER, Probes.KERNEL, Probes.HOST, Probes.RESOURCES)) {
            long n = Probes.REACH.entrySet().stream().filter(e -> e.getValue().equals(reach) && r.get(e.getKey())).count();
            parts.add(reach.substring(reach.lastIndexOf(' ') + 1) + " " + n);
        }
        line("    " + String.join(" + ", parts) + " = " + Probes.possible(r) + " of " + Probes.NAMES.length);
    }

    /** The values the probes read, with what they mean. */
    void facts(Map<String, String> f) {
        line(String.format("    %-12s %s", "id -u", f.get("id-u") + ("0".equals(f.get("id-u")) ? " (root)" : " (not root)")));
        line(String.format("    %-12s %s", "CapEff", f.get("CapEff") + " (" + Capabilities.bits(f.get("CapEff")).size() + " capabilities)"));
        line(String.format("    %-12s %s", "Seccomp", f.get("Seccomp") + ("0".equals(f.get("Seccomp")) ? " (no filter)" : " (a filter is on)")));
        String mem = f.get("memory.max");
        line(String.format("    %-12s %s", "memory.max", "max".equals(mem) ? "max (no limit)"
                : mem + " (" + mem + " / 1048576 = " + Long.parseLong(mem) / 1048576 + " MiB)"));
        String pids = f.get("pids.max");
        line(String.format("    %-12s %s", "pids.max", "max".equals(pids) ? "max (no limit)" : pids + " processes"));
    }

    /** How a CapEff mask is read: its set bits, their names, and the two bits the probes test. */
    void capabilityBits(String capEff) {
        List<String> bits = Capabilities.bits(capEff).stream().map(String::valueOf).toList();
        wrap("    bits set   ", bits.isEmpty() ? List.of("none") : bits);
        wrap("    names      ", Capabilities.names(capEff).isEmpty() ? List.of("none") : Capabilities.names(capEff));
        long mask = Long.parseUnsignedLong(capEff, 16);
        for (int n : List.of(Capabilities.NET_RAW, Capabilities.SYS_ADMIN)) {
            boolean held = Capabilities.held(capEff, n);
            line(String.format("    %-10s (0x%x >> %d) & 1 = %d, %s", Capabilities.NAMES.get(n), mask, n, held ? 1 : 0, held ? "held" : "not held"));
        }
    }

    /** One hardening step: the count after it, and what it blocked and opened (worked out from the two lists). */
    void step(int n, String label, Map<String, Boolean> before, Map<String, Boolean> after) {
        line(String.format("    %d %-31s %2d of %d", n, label, Probes.possible(after), Probes.NAMES.length));
        List<String> blocked = new ArrayList<>(Probes.possibleNames(before));
        blocked.removeAll(Probes.possibleNames(after));
        List<String> opened = new ArrayList<>(Probes.possibleNames(after));
        opened.removeAll(Probes.possibleNames(before));
        wrap("        blocks ", blocked.isEmpty() ? List.of("nothing") : blocked);
        if (!opened.isEmpty()) {
            wrap("        opens  ", opened);
        }
    }

    /** What an added option changed, against the container before it. */
    void changed(Map<String, Boolean> before, Map<String, Boolean> after) {
        List<String> opened = new ArrayList<>(Probes.possibleNames(after));
        opened.removeAll(Probes.possibleNames(before));
        line(String.format("    %d of %d possible", Probes.possible(after), Probes.NAMES.length));
        wrap("    newly possible: ", opened.isEmpty() ? List.of("nothing") : opened);
    }

    /** One row of the admission table: a pod and the decision of each namespace. */
    void admission(String pod, List<String> decisions) {
        StringBuilder row = new StringBuilder(String.format("    %-11s", pod));
        decisions.forEach(d -> row.append(String.format(" %-13s", d)));
        line(row.toString().stripTrailing());
    }
}
