package com.jk.explore.security.controlsevidence;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public class ControlsAndEvidenceDemo {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    static final List<String> FUNCTIONS = List.of("GV", "ID", "PR", "DE", "RS", "RC");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static Map<Assessor.Status, List<String>> byStatus(Assessor assessor, List<Control> controls) throws Exception {
        Map<Assessor.Status, List<String>> map = new EnumMap<>(Assessor.Status.class);
        for (Control c : controls) {
            map.computeIfAbsent(assessor.assess(c), k -> new ArrayList<>()).add(c.id());
        }
        return map;
    }

    static String summary(Map<Assessor.Status, List<String>> m) {
        List<String> parts = new ArrayList<>();
        for (Assessor.Status s : Assessor.Status.values()) {
            parts.add(s.name().toLowerCase() + " " + m.getOrDefault(s, List.of()).size());
        }
        return String.join(", ", parts);
    }

    static Map<String, Long> perFunction(List<Control> controls) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String f : FUNCTIONS) {
            m.put(f, controls.stream().filter(c -> c.function().equals(f)).count());
        }
        return m;
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        List<Control> controls = Control.load(Path.of("catalogue/controls.csv"));
        List<String> applicable = Control.applicable(Path.of("catalogue/applicable.txt"));
        Assessor assessor = new Assessor(Path.of("evidence"), TODAY);

        out.append("--- the catalogue and its maps\n");
        out.append("controls: " + controls.size() + ", owners: " + controls.stream().map(Control::owner).distinct().count() + "\n");
        out.append("NIST CSF 2.0 functions with a control: " + perFunction(controls).values().stream().filter(n -> n > 0).count() + " of " + FUNCTIONS.size() + ", controls in each: " + perFunction(controls) + "\n");
        TreeSet<String> covered = new TreeSet<>();
        controls.forEach(c -> covered.addAll(c.iso()));
        List<String> uncovered = applicable.stream().filter(a -> !covered.contains(a)).toList();
        out.append("ISO 27001 Annex A controls the bank has chosen: " + applicable.size() + ", served by a control: " + (applicable.size() - uncovered.size()) + ", not served: " + uncovered + "\n");
        out.append("controls that serve more than one Annex A control: " + controls.stream().filter(c -> c.iso().size() > 1).map(Control::id).toList() + "\n");

        out.append("--- what the evidence says today (" + TODAY + ")\n");
        Map<Assessor.Status, List<String>> status = byStatus(assessor, controls);
        out.append("automated evidence: " + controls.stream().filter(Control::automated).count() + " controls, attested by a person: " + controls.stream().filter(c -> c.kind().equals("manual")).count() + ", no evidence: " + controls.stream().filter(c -> c.kind().equals("none")).count() + "\n");
        out.append("status: " + summary(status) + "\n");
        for (Assessor.Status s : List.of(Assessor.Status.FAILING, Assessor.Status.PARTIAL, Assessor.Status.STALE, Assessor.Status.NONE)) {
            out.append(s.name().toLowerCase() + ": " + status.getOrDefault(s, List.of()) + "\n");
        }
        long good = status.getOrDefault(Assessor.Status.PASSING, List.of()).size();
        out.append("controls with fresh passing test evidence: " + good + " of " + controls.size() + "\n");

        out.append("--- the trail for one control\n");
        Control tls = controls.stream().filter(c -> c.id().equals("C03")).findFirst().orElseThrow();
        Evidence e = assessor.evidenceOf(tls).orElseThrow();
        out.append(tls.id() + " " + tls.name() + ", owner " + tls.owner() + ", CSF " + tls.csf() + ", ISO " + String.join(" and ", tls.iso()) + "\n");
        out.append("evidence: " + e.suite() + ", ran " + assessor.ageInDays(e) + " days ago, " + e.tests() + " tests, " + e.failed().size() + " failed: " + e.failed() + "\n");
        Evidence oldest = null;
        Control oldestControl = null;
        for (Control c : controls) {
            Optional<Evidence> ev = assessor.evidenceOf(c);
            if (c.automated() && ev.isPresent() && (oldest == null || ev.get().ran().isBefore(oldest.ran()))) {
                oldest = ev.get();
                oldestControl = c;
            }
        }
        out.append("the oldest automated evidence: " + oldestControl.id() + ", " + assessor.ageInDays(oldest) + " days, the limit is " + Assessor.AUTOMATED_DAYS + "\n");

        out.append("--- when a test starts to fail\n");
        Control least = controls.get(0);
        Evidence ok = assessor.evidenceOf(least).orElseThrow();
        Evidence broken = new Evidence(ok.suite(), ok.ran(), ok.tests(), ok.skipped(), List.of("deniesAnAdminRoleToAClerk"));
        out.append(least.id() + " before: " + assessor.assess(least) + ", after one failing test: " + assessor.assess(least, Optional.of(broken)) + "\n");
        long pr = controls.stream().filter(c -> c.function().equals("PR") && status.getOrDefault(Assessor.Status.PASSING, List.of()).contains(c.id())).count();
        out.append("controls of the Protect function with passing evidence: " + pr + " of " + perFunction(controls).get("PR") + " now, " + (pr - 1) + " after\n");

        out.append("--- the bill\n");
        out.append("evidence shows that a test passed, not that the control works: a test can be too weak\n");
        out.append("a person's attestation is evidence of a promise; it ages more slowly and proves less\n");
        out.append("a control with no owner and no date is a wish: every line here has both\n");
        return out.toString();
    }
}
