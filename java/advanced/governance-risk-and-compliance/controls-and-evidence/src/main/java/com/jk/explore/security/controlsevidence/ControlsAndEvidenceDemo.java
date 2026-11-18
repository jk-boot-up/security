package com.jk.explore.security.controlsevidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/** Prints the five acts. The day is fixed and the evidence is fixtures, so every run prints the same text. */
public class ControlsAndEvidenceDemo {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    static final List<String> FUNCTIONS = List.of("GV", "ID", "PR", "DE", "RS", "RC");
    static final int XML_LINES = 7;

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

    static List<String> inFunction(List<Control> controls, String function) {
        return controls.stream().filter(c -> c.function().equals(function)).map(Control::id).toList();
    }

    static void steps(StringBuilder out, List<StatusSteps.Step> steps) {
        for (StatusSteps.Step s : steps) {
            out.append(String.format("    %-17s %s%s%n", s.question(), s.answer(), s.ends() == null ? "" : "  -> " + s.ends()));
        }
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        List<Control> controls = Control.load(Path.of("catalogue/controls.csv"));
        List<String> applicable = Control.applicable(Path.of("catalogue/applicable.txt"));
        Assessor assessor = new Assessor(Path.of("evidence"), TODAY);
        Map<Assessor.Status, List<String>> status = byStatus(assessor, controls);
        List<String> passing = status.getOrDefault(Assessor.Status.PASSING, List.of());

        // ONE. One control, two frameworks.
        Control tls = controls.stream().filter(c -> c.id().equals("C03")).findFirst().orElseThrow();
        out.append("ONE. One control, mapped to two frameworks.\n");
        out.append("  THE INPUT, ONE LINE OF THE CATALOGUE (controls.csv):\n");
        out.append(String.format("    %-9s %s%n", "id", tls.id()));
        out.append(String.format("    %-9s %s%n", "name", tls.name()));
        out.append(String.format("    %-9s %s%n", "owner", tls.owner()));
        out.append(String.format("    %-9s %s%n", "csf", tls.csf()));
        out.append(String.format("    %-9s %s%n", "iso", String.join(";", tls.iso())));
        out.append(String.format("    %-9s %s%n", "evidence", tls.evidence()));
        out.append(String.format("    %-9s %s (a test report)%n", "kind", tls.kind()));
        Frameworks.CsfId csf = Frameworks.CsfId.parse(tls.csf());
        out.append("  HOW THE NIST CSF 2.0 ID READS, " + tls.csf() + ":\n");
        out.append(String.format("    %-12s %-3s %s%n", "function", csf.function(), Frameworks.FUNCTIONS.get(csf.function())));
        out.append(String.format("    %-12s %-3s %s%n", "category", csf.category(), Frameworks.CATEGORIES.get(csf.function() + "." + csf.category())));
        out.append(String.format("    %-12s %-3s %s%n", "subcategory", csf.number(), "outcome " + Integer.parseInt(csf.number()) + " of the category"));
        out.append("  HOW THE ISO 27001 ANNEX A IDS READ:\n");
        for (String a : tls.iso()) {
            Frameworks.AnnexId id = Frameworks.AnnexId.parse(a);
            out.append(String.format("    %-7s theme %d (%s), control %d%n", a, id.theme(), Frameworks.THEMES.get(id.theme()), id.number()));
            out.append(String.format("    %-7s %s%n", "", Frameworks.ANNEX_A.get(a)));
        }
        out.append("  THE RESULT, ONE PROMISE IN TWO LANGUAGES:\n");
        out.append("    " + tls.id() + " answers CSF " + tls.csf() + " and ISO " + String.join(" and ", tls.iso()) + "\n");
        out.append("    one test report is the evidence for all three\n");

        // TWO. The maps.
        out.append("TWO. The maps: every control, both frameworks.\n");
        out.append("  THE INPUT:\n");
        out.append("    controls         " + controls.size() + ", owners " + controls.stream().map(Control::owner).distinct().count() + "\n");
        out.append("    Annex A chosen   " + applicable.size() + " (the statement of applicability)\n");
        out.append("  NIST CSF 2.0, THE CONTROLS IN EACH FUNCTION:\n");
        for (String f : FUNCTIONS) {
            List<String> ids = inFunction(controls, f);
            out.append(String.format("    %-3s %-9s %d  %s%n", f, Frameworks.FUNCTIONS.get(f), ids.size(), String.join(" ", ids)));
        }
        TreeSet<String> covered = new TreeSet<>();
        controls.forEach(c -> covered.addAll(c.iso()));
        List<String> uncovered = applicable.stream().filter(a -> !covered.contains(a)).toList();
        out.append("  ISO 27001 ANNEX A, THE " + applicable.size() + " THE BANK CHOSE:\n");
        out.append("    served by a control  " + (applicable.size() - uncovered.size()) + " of " + applicable.size() + ":\n");
        List<String> served = applicable.stream().filter(covered::contains).toList();
        int half = (served.size() + 1) / 2;
        out.append("      " + String.join(" ", served.subList(0, half)) + "\n");
        out.append("      " + String.join(" ", served.subList(half, served.size())) + "\n");
        for (String u : uncovered) {
            out.append("    not served           " + u + " " + Frameworks.ANNEX_A.get(u) + "\n");
        }
        for (Control c : controls.stream().filter(c -> c.iso().size() > 1).toList()) {
            out.append("    two at once          " + c.id() + " serves " + String.join(" and ", c.iso()) + "\n");
        }
        long functions = FUNCTIONS.stream().filter(f -> !inFunction(controls, f).isEmpty()).count();
        out.append("  THE RESULT:\n");
        out.append("    CSF functions with a control  " + functions + " of " + FUNCTIONS.size() + "\n");
        out.append("    Annex A controls served       " + (applicable.size() - uncovered.size()) + " of " + applicable.size() + ", gap: " + String.join(", ", uncovered) + "\n");

        // THREE. One test report.
        Evidence e = assessor.evidenceOf(tls).orElseThrow();
        List<String> xml = Files.readAllLines(Path.of("evidence", tls.evidence()));
        out.append("THREE. Evidence from a test report.\n");
        out.append("  THE INPUT, evidence/" + tls.evidence() + ":\n");
        for (String line : xml.subList(1, 1 + XML_LINES)) {
            out.append("    " + line + "\n");
        }
        out.append("      ... " + (e.tests() - e.failed().size()) + " more testcases, none with a failure\n");
        out.append("  WHAT THE READER TAKES FROM IT:\n");
        out.append(String.format("    %-7s %s%n", "suite", e.suite()));
        out.append(String.format("    %-7s %s (the date of the timestamp)%n", "ran", e.ran()));
        out.append(String.format("    %-7s %d, skipped %d%n", "tests", e.tests(), e.skipped()));
        out.append(String.format("    %-7s %s%n", "failed", String.join(", ", e.failed())));
        out.append("  HOW THE STATUS IS DECIDED, TODAY " + TODAY + ":\n");
        out.append("    age = " + TODAY + " - " + e.ran() + " = " + assessor.ageInDays(e) + " days\n");
        steps(out, StatusSteps.of(assessor, tls, Optional.of(e)));
        out.append("  THE RESULT:\n");
        out.append("    " + tls.id() + " " + assessor.assess(tls) + ", and the test that failed is named\n");

        // FOUR. Every control today.
        out.append("FOUR. Every control today (" + TODAY + ").\n");
        out.append("  THE INPUT:\n");
        out.append("    test reports " + controls.stream().filter(Control::automated).count() + ", attestations " + controls.stream().filter(c -> c.kind().equals("manual")).count()
                + ", nothing " + controls.stream().filter(c -> c.kind().equals("none")).count() + "\n");
        out.append("  THE RULES, IN ORDER:\n");
        out.append("    no evidence                          NONE\n");
        out.append("    older than " + Assessor.AUTOMATED_DAYS + " days (" + Assessor.MANUAL_DAYS + " if signed)   STALE\n");
        out.append("    signed by a person                   ATTESTED\n");
        out.append("    a test failed                        FAILING\n");
        out.append("    a test skipped                       PARTIAL\n");
        out.append("    otherwise                            PASSING\n");
        out.append("  EACH CONTROL:\n");
        for (Control c : controls) {
            Optional<Evidence> ev = assessor.evidenceOf(c);
            String age = ev.map(x -> assessor.ageInDays(x) + "d").orElse("-");
            String what;
            if (ev.isEmpty()) {
                what = "no evidence";
            } else if (!c.automated()) {
                what = "signed " + ev.get().ran();
            } else if (!ev.get().failed().isEmpty()) {
                what = ev.get().failed().size() + " of " + ev.get().tests() + " failed";
            } else if (ev.get().skipped() > 0) {
                what = ev.get().skipped() + " of " + ev.get().tests() + " skipped";
            } else {
                what = ev.get().tests() + " tests passed";
            }
            out.append(String.format("    %-4s %-3s %5s  %-18s %s%n", c.id(), c.function(), age, what, assessor.assess(c)));
        }
        out.append("  THE RESULT:\n");
        out.append("    " + summary(status) + "\n");
        out.append("    fresh passing test evidence: " + passing.size() + " of " + controls.size() + "\n");

        // FIVE. A test starts to fail.
        Control least = controls.get(0);
        Evidence ok = assessor.evidenceOf(least).orElseThrow();
        Evidence broken = new Evidence(ok.suite(), ok.ran(), ok.tests(), ok.skipped(), List.of("deniesAnAdminRoleToAClerk"));
        List<String> protectPassing = inFunction(controls, "PR").stream().filter(passing::contains).toList();
        List<String> protectAfter = protectPassing.stream().filter(id -> !id.equals(least.id())).toList();
        int protect = inFunction(controls, "PR").size();
        out.append("FIVE. When a test starts to fail.\n");
        out.append("  BEFORE, " + least.id() + " " + least.name() + ":\n");
        out.append(String.format("    %s, %d tests, %d failed: %s%n", ok.suite(), ok.tests(), ok.failed().size(), assessor.assess(least)));
        out.append("    Protect with passing evidence: " + protectPassing.size() + " of " + protect + " (" + String.join(", ", protectPassing) + ")\n");
        out.append("  THE CHANGE: a role change lets a clerk act as an admin\n");
        out.append("    the next build runs the same " + ok.tests() + " tests; one fails\n");
        out.append("  AFTER:\n");
        steps(out, StatusSteps.of(assessor, least, Optional.of(broken)).subList(2, 3));
        out.append("    Protect with passing evidence: " + protectAfter.size() + " of " + protect + " (" + String.join(", ", protectAfter) + ")\n");
        out.append("    nobody had to update a spreadsheet\n");
        out.append("  WHAT EVIDENCE CANNOT SHOW:\n");
        out.append("    a passing test, not a working control: tests can be weak\n");
        out.append("    an attestation is a promise: it ages slowly, proves less\n");
        out.append("    a control with no owner and no date is a wish\n");
        return out.toString();
    }
}
