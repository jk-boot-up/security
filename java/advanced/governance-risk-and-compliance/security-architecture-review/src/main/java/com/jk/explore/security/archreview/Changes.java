package com.jk.explore.security.archreview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** What changed between two versions of a design: part properties, flows and decision records. */
public final class Changes {

    /** One change: the part, flow or record, what changed, and its value before and after. */
    public record Change(String subject, String key, String before, String after) {
    }

    private Changes() {
    }

    private static String value(Map<String, String> props, String key) {
        return props.getOrDefault(key, "-");
    }

    private static void compare(String subject, Map<String, String> a, Map<String, String> b, List<Change> out) {
        TreeSet<String> keys = new TreeSet<>(a.keySet());
        keys.addAll(b.keySet());
        keys.remove("id");
        for (String k : keys) {
            if (!value(a, k).equals(value(b, k))) {
                out.add(new Change(subject, k, value(a, k), value(b, k)));
            }
        }
    }

    public static List<Change> parts(Design before, Design after) {
        List<Change> out = new ArrayList<>();
        for (Design.Part p : before.parts()) {
            compare(p.id(), p.props(), after.part(p.id()).props(), out);
        }
        return out;
    }

    public static List<Change> flows(Design before, Design after) {
        List<Change> out = new ArrayList<>();
        TreeSet<String> ids = new TreeSet<>();
        before.flows().forEach(f -> ids.add(f.id()));
        after.flows().forEach(f -> ids.add(f.id()));
        for (String id : ids) {
            Design.Flow a = before.flows().stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
            Design.Flow b = after.flows().stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
            if (b == null) {
                out.add(new Change(id, "flow", "drawn", "removed"));
            } else if (a == null) {
                out.add(new Change(id, "flow", "-", "added"));
            } else {
                compare(id, a.props(), b.props(), out);
            }
        }
        return out;
    }

    public static List<Change> decisions(Map<String, Adr> before, Map<String, Adr> after) {
        List<Change> out = new ArrayList<>();
        TreeSet<String> numbers = new TreeSet<>(before.keySet());
        numbers.addAll(after.keySet());
        for (String n : numbers) {
            String a = before.containsKey(n) ? before.get(n).status() : "-";
            String b = after.containsKey(n) ? after.get(n).status() : "removed";
            if (!a.equals(b)) {
                out.add(new Change(n, "status", a, b));
            }
        }
        return out;
    }
}
