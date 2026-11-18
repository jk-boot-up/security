package com.jk.explore.security.archreview;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Reviews the bank's first design with rules, scores them on the planted weaknesses, and checks the reviewed design. */
public class ArchitectureReviewDemo {

    private final StringBuilder out = new StringBuilder();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        ArchitectureReviewDemo demo = new ArchitectureReviewDemo();
        Design v1 = Design.load(Path.of("design/v1/bank.dsl"));
        Map<String, Adr> adrs1 = Adr.load(Path.of("design/v1/adr"));
        Design v2 = Design.load(Path.of("design/v2/bank.dsl"));
        Map<String, Adr> adrs2 = Adr.load(Path.of("design/v2/adr"));
        demo.asDrawn(v1, adrs1);
        List<Finding> first = Rules.run(v1, adrs1, Rules.FIRST_SET);
        demo.rules(v1, adrs1, first);
        List<Finding> all = Rules.run(v1, adrs1, Rules.ALL);
        demo.oneMoreRule(v1, first, all);
        demo.reviewed(v1, adrs1, v2, adrs2, all);
        demo.bill(v2, adrs2, all);
        return demo.out.toString();
    }

    private void act(String number, String title) {
        out.append(number).append(". ").append(title).append(".\n");
    }

    private void section(String header) {
        out.append("  ").append(header).append(":\n");
    }

    private void line(String format, Object... args) {
        out.append("    ").append(String.format(format, args).stripTrailing()).append("\n");
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    /** Planted weaknesses that a list of findings reports, by the same rule on the same subject. */
    static long found(List<Finding> findings) {
        return Planted.FIRST_DESIGN.stream().filter(p -> p.rule() != null && p.reportedBy(findings)).count();
    }

    /** The number (1 to 10) of the planted weakness a finding reports, or 0. */
    static int plantedNumber(Finding f) {
        for (int i = 0; i < Planted.FIRST_DESIGN.size(); i++) {
            Planted p = Planted.FIRST_DESIGN.get(i);
            if (f.rule().equals(p.rule()) && f.subject().equals(p.subject())) {
                return i + 1;
            }
        }
        return 0;
    }

    static long dmzToConfidential(Design d) {
        return d.flows().stream().filter(f -> f.to().prop("classification").equals("confidential") && f.from().zone().equals("dmz")).count();
    }

    private void score(List<Finding> findings) {
        long hits = found(findings);
        long other = findings.stream().filter(f -> plantedNumber(f) == 0).count();
        line("%-36s%d", "planted weaknesses", Planted.FIRST_DESIGN.size());
        line("%-36s%d", "findings", findings.size());
        line("%-36s%d", "planted ones found (rule, subject)", hits);
        line("%-36s%d", "findings that match no planted one", other);
        line("%-36s%d", "planted ones missed", Planted.FIRST_DESIGN.size() - hits);
    }

    private void missed(List<Finding> findings) {
        for (int i = 0; i < Planted.FIRST_DESIGN.size(); i++) {
            Planted p = Planted.FIRST_DESIGN.get(i);
            if (p.rule() == null || !p.reportedBy(findings)) {
                line("%-4d%-17s%s", i + 1, p.subject(), p.description());
            }
        }
    }

    private void asDrawn(Design v1, Map<String, Adr> adrs) {
        act("ONE", "The design as drawn");
        section("THE PARTS (design/v1/bank.dsl)");
        line("%-10s%-11s%-10s%-15s%s", "id", "kind", "zone", "owner", "data");
        for (Design.Part p : v1.parts()) {
            line("%-10s%-11s%-10s%-15s%s", p.id(), p.kind().name().toLowerCase(), p.zone(), p.prop("owner").isEmpty() ? "-" : p.prop("owner"),
                    p.prop("classification").isEmpty() ? "-" : p.prop("classification"));
        }
        section("THE FLOWS");
        line("%-21s%-18s%-11s%s", "flow", "zones", "encrypted", "caller check");
        for (Design.Flow f : v1.flows()) {
            FlowCheck c = new FlowCheck(f);
            line("%-21s%-18s%-11s%s", f.id(), c.zones(), yesNo(c.encrypted()), c.callerCheck());
        }
        line("%d flows: %d cross a zone, %d inside one zone", v1.flows().size(), v1.flows().stream().filter(f -> new FlowCheck(f).crossesZone()).count(),
                v1.flows().stream().filter(f -> !new FlowCheck(f).crossesZone()).count());
        section("THE DECISION RECORDS (design/v1/adr)");
        line("%-6s%-17s%-10s%s", "adr", "status", "used by", "title");
        for (Map.Entry<String, Adr> e : adrs.entrySet()) {
            String usedBy = String.join(",", v1.parts().stream().filter(p -> p.prop("adr").equals(e.getKey())).map(Design.Part::id).toList());
            line("%-6s%-17s%-10s%s", e.getKey(), e.getValue().status(), usedBy.isEmpty() ? "-" : usedBy, e.getValue().title());
        }
        section("HOW THE STRIDE QUESTIONS ADD UP");
        line("%-20s%-7s%-13s%s", "asked about", "count", "letters", "questions");
        long outside = v1.parts().stream().filter(p -> p.kind() == Design.Kind.PERSON || p.kind() == Design.Kind.EXTERNAL).count();
        stride("person or external", outside, Stride.letters(Design.Kind.PERSON));
        stride("process", count(v1, Design.Kind.PROCESS), Stride.letters(Design.Kind.PROCESS));
        stride("data store", count(v1, Design.Kind.DATASTORE), Stride.letters(Design.Kind.DATASTORE));
        stride("flow", v1.flows().size(), Stride.FLOW);
        line("%-40s%d", "total", Stride.questions(v1));
    }

    private static long count(Design d, Design.Kind kind) {
        return d.parts().stream().filter(p -> p.kind() == kind).count();
    }

    private void stride(String what, long n, List<String> letters) {
        line("%-20s%-7d%-13s%d", what, n, String.join(",", letters), n * letters.size());
    }

    private void rules(Design v1, Map<String, Adr> adrs, List<Finding> first) {
        act("TWO", "Rules over the design");
        section("THE PLANTED WEAKNESSES (the truth)");
        for (int i = 0; i < Planted.FIRST_DESIGN.size(); i++) {
            Planted p = Planted.FIRST_DESIGN.get(i);
            line("%-4d%-17s%s", i + 1, p.subject(), p.description());
        }
        section("THE FIRST " + Rules.FIRST_SET.size() + " RULES");
        for (String r : Rules.FIRST_SET) {
            line("%-32s%s", r, Rules.WANTS.get(r));
        }
        section("HOW TWO FLOWS ARE CHECKED");
        FlowCheck a = new FlowCheck(v1.flows().stream().filter(f -> f.id().equals("web -> ledgerdb")).findFirst().orElseThrow());
        FlowCheck b = new FlowCheck(v1.flows().stream().filter(f -> f.id().equals("web -> sessions")).findFirst().orElseThrow());
        line("%-24s%-18s%s", "question", a.flow().id(), b.flow().id());
        line("%-24s%-18s%s", "zones", a.zones(), b.zones());
        line("%-24s%-18s%s", "crosses a zone?", yesNo(a.crossesZone()), yesNo(b.crossesZone()));
        line("%-24s%-18s%s", "encrypted?", yesNo(a.encrypted()), yesNo(b.encrypted()));
        line("%-24s%-18s%s", "caller check?", a.callerCheck(), b.callerCheck());
        line("%-24s%-18s%s", "to confidential data?", yesNo(a.toConfidentialStore()), yesNo(b.toConfidentialStore()));
        line("%-24s%-18s%s", "from an outer zone?", yesNo(a.fromOuterZone()), yesNo(b.fromOuterZone()));
        for (FlowCheck c : List.of(a, b)) {
            line("%-18s%s", c.flow().id(), String.join(", ", c.rulesThatFire().stream().filter(Rules.FIRST_SET::contains).toList()));
        }
        section("HOW TWO DECISIONS ARE CHECKED");
        for (String id : List.of("sessions", "cards")) {
            String n = v1.part(id).prop("adr");
            line("%-10s rests on %s: %s", id, n, adrs.get(n).status());
        }
        section("THE FINDINGS");
        line("%-32s%-17s%s", "rule", "subject", "planted");
        for (Finding f : first) {
            int n = plantedNumber(f);
            line("%-32s%-17s%s", f.rule(), f.subject(), n == 0 ? "-" : String.valueOf(n));
        }
        section("THE SCORE");
        score(first);
        section("NOT REPORTED");
        missed(first);
    }

    private void oneMoreRule(Design v1, List<Finding> first, List<Finding> all) {
        act("THREE", "One more rule");
        String rule = "every-flow-encrypted-and-authenticated";
        section("THE NEW RULE");
        line("%s", rule);
        line("wants: every flow encrypted and checking its caller,");
        line("       inside a zone too");
        section("THE FLOWS INSIDE ONE ZONE (the cross-zone rules skip them)");
        for (Design.Flow f : v1.flows()) {
            FlowCheck c = new FlowCheck(f);
            if (!c.crossesZone()) {
                line("%-16s%-12s encrypted %s, caller check %s", f.id(), c.zones(), yesNo(c.encrypted()), c.callerCheck());
            }
        }
        section("THE NEW FINDINGS");
        line("%-17s%s", "subject", "planted, or found already by");
        for (Finding f : all) {
            if (f.rule().equals(rule)) {
                int n = plantedNumber(f);
                String also = String.join(", ", first.stream().filter(g -> g.subject().equals(f.subject())).map(Finding::rule).toList());
                line("%-17s%s", f.subject(), n != 0 ? "planted " + n : also);
            }
        }
        section("THE SCORE WITH " + Rules.ALL.size() + " RULES");
        score(all);
        section("NOT REPORTED");
        missed(all);
        line("the model has no property for what a page trusts");
    }

    private void reviewed(Design v1, Map<String, Adr> adrs1, Design v2, Map<String, Adr> adrs2, List<Finding> all) {
        act("FOUR", "The reviewed design");
        section("BEFORE THE REVIEW (design/v1)");
        state(v1, adrs1, all);
        section("THE PARTS THAT CHANGED");
        for (Changes.Change c : Changes.parts(v1, v2)) {
            line("%-10s%-19s%s -> %s", c.subject(), c.key(), c.before(), c.after());
        }
        section("THE FLOWS THAT CHANGED");
        for (Changes.Change c : Changes.flows(v1, v2)) {
            line("%-17s%-11s%s -> %s", c.subject(), c.key(), c.before(), c.after());
        }
        section("THE DECISION RECORDS THAT CHANGED");
        for (Changes.Change c : Changes.decisions(adrs1, adrs2)) {
            line("%-6s%s -> %s", c.subject(), c.before(), c.after());
        }
        section("AFTER THE REVIEW (design/v2)");
        state(v2, adrs2, Rules.run(v2, adrs2, Rules.ALL));
    }

    private void state(Design d, Map<String, Adr> adrs, List<Finding> findings) {
        line("%-36s%d", "parts", d.parts().size());
        line("%-36s%d (%d cross a zone)", "flows", d.flows().size(), d.flows().stream().filter(f -> new FlowCheck(f).crossesZone()).count());
        line("%-36s%d", "findings from all " + Rules.ALL.size() + " rules", findings.size());
        line("%-36s%d", "flows from the dmz to confidential", dmzToConfidential(d));
        line("%-36s%d of %d", "decision records accepted", adrs.values().stream().filter(Adr::accepted).count(), adrs.size());
    }

    private void bill(Design v2, Map<String, Adr> adrs2, List<Finding> all) {
        act("FIVE", "The bill");
        section("WHAT THE RULES GAVE");
        line("%-34s%d", "rules written", Rules.ALL.size());
        line("%-34s%d of %d", "planted weaknesses found", found(all), Planted.FIRST_DESIGN.size());
        line("%-34s%d", "findings in the reviewed design", Rules.run(v2, adrs2, Rules.ALL).size());
        section("WHAT IS STILL LEFT TO PEOPLE");
        line("%-34s%d", "STRIDE questions, reviewed design", Stride.questions(v2));
        line("%-34s%d", "planted ones no rule can find", Planted.FIRST_DESIGN.stream().filter(p -> p.rule() == null).count());
        line("does the built system match the model?");
        line("are the Accepted decisions right?");
        section("THE LESSON");
        line("rules read the model, and the model is what the team drew:");
        line("a flaw that is not in the picture is not found");
        line("no findings means the rules agree, not that it is safe");
    }
}
