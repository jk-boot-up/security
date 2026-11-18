package com.jk.explore.security.pytmmodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PytmDemo {

    static final List<String> SEVERITY_ORDER = List.of("Very High", "High", "Medium", "Low", "Very Low");
    /** What the plain threat-modeling project reports for the same bank, for comparison. */
    static final int PLAIN_DRAFT_PROBLEMS = 2;
    static final int PLAIN_STRIDE_CANDIDATES = 20;
    static final String TEMPLATE = "# {tm.name}\n\n{tm.description}\n\n## Findings\n\n{findings:repeat:* {{item.threat_id}} {{item.target}}: {{item.description}}\n}\n";

    public static void main(String[] args) throws Exception {
        Pytm pytm = new Pytm(Path.of("build"));
        System.out.print(run(pytm));
        Path svg = pytm.render("bank", pytm.dfd("bank-dfd", model(Design.bank(), Answers.Stage.RESILIENCE)));
        System.err.println("(the diagram, drawn by Graphviz: " + svg + ")");
    }

    public static String run(ThreatTool pytm) throws Exception {
        StringBuilder out = new StringBuilder();
        List<ThreatTool.Threat> library = pytm.library();
        act1(out, pytm, library);
        act2(out, pytm, library);
        act3(out, pytm, library);
        act4(out, pytm);
        act5(out, pytm, library);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String model(Design design, Answers.Stage stage) {
        return Writer.python(design, stage, "Bank payments");
    }

    static ThreatTool.Threat threat(List<ThreatTool.Threat> library, String id) {
        return library.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    }

    /** The library threats that apply to a kind of element, by pytm's class name. */
    static long applying(List<ThreatTool.Threat> library, String kind) {
        return library.stream().filter(t -> t.targets().contains(kind)).count();
    }

    /** A condition's text without the "target." and "controls." prefixes, as the pictures show it. */
    static String brief(String condition) {
        return condition.replace("target.controls.", "").replace("target.", "");
    }

    private static void act1(StringBuilder out, ThreatTool pytm, List<ThreatTool.Threat> library) throws Exception {
        line(out, "ONE. The bank, written for pytm.");
        Design bank = Design.bank();
        line(out, "THE DESIGN (Java records: " + bank.elements().size() + " elements in " + bank.boundaries().size() + " boundaries):");
        for (Design.Element e : bank.elements()) {
            line(out, String.format("  %-9s %-10s %-15s %s", e.boundary(), e.id(), e.kind().pytm, e.name()));
        }
        line(out, "THE FLOWS (" + bank.flows().size() + " arrows, each with its protocol and port):");
        for (Design.Flow f : bank.flows()) {
            line(out, String.format("  %-2s  %-21s  %-10s  %s", f.id(), f.from() + " -> " + f.to(), f.protocol() + ":" + f.port(), f.label()));
        }
        String python = model(bank, Answers.Stage.NONE);
        line(out, "THE MODEL (Python written by the program: " + python.lines().count() + " lines):");
        List<Predicate<String>> shown = List.of(l -> l.startsWith("tm = TM("), l -> l.startsWith("dmz = "), l -> l.startsWith("gateway = "),
                l -> l.startsWith("gateway.inBoundary"), l -> l.startsWith("f2 = "), l -> l.startsWith("f2.protocol"), l -> l.equals("tm.process()"));
        python.lines().filter(l -> shown.stream().anyMatch(p -> p.test(l))).forEach(l -> line(out, "  " + l));
        ThreatTool.Threat ac06 = threat(library, "AC06");
        line(out, "THE LIBRARY (pytm " + Pytm.VERSION + ", what it checks the model against):");
        line(out, "  threats     " + library.size() + ", each a kind, a severity and a condition");
        line(out, "  one         " + ac06.id() + " " + ac06.title() + ", " + ac06.severity());
        line(out, "  applies to  every " + String.join(", ", ac06.targets()));
        List<String> parts = List.of(brief(ac06.condition()).split(" or "));
        line(out, "  condition   " + parts.getFirst());
        parts.stream().skip(1).forEach(p -> line(out, "              or " + p));
        line(out, "THE DRAFT (the plain project's: a card vault the app writes to):");
        Design draft = Design.draft();
        Design.Element vault = draft.elements().getLast();
        Design.Flow d1 = draft.flows().getLast();
        line(out, "  " + vault.id() + "  " + vault.kind().pytm + " in " + vault.boundary() + ": " + vault.name());
        line(out, String.format("  %-2s  %-21s  %-10s  %s", d1.id(), d1.from() + " -> " + d1.to(), d1.protocol() + ":" + d1.port(), d1.label()));
        List<ThreatTool.Finding> found = pytm.findings("draft", model(draft, Answers.Stage.NONE));
        long onVault = found.stream().filter(f -> f.target().equals(vault.name())).count();
        long onFlow = found.stream().filter(f -> f.target().equals(d1.label())).count();
        line(out, "THE RESULT:");
        line(out, "  pytm           runs it without a complaint");
        line(out, "  new findings   " + (onVault + onFlow) + ": " + onVault + " on " + vault.name() + ", " + onFlow + " on " + d1.label());
        line(out, "  plain check    " + PLAIN_DRAFT_PROBLEMS + " problems: a skipped boundary, no owner");
        line(out, "  so             pytm reads answers, not the picture's shape");
    }

    private static void act2(StringBuilder out, ThreatTool pytm, List<ThreatTool.Threat> library) throws Exception {
        line(out, "TWO. What pytm finds when nothing is said.");
        Design bank = Design.bank();
        String python = model(bank, Answers.Stage.NONE);
        List<ThreatTool.Finding> f = pytm.findings("bank-none", python);
        line(out, "THE INPUT (the model with no answers):");
        line(out, "  answers   " + python.lines().filter(l -> l.contains(".controls.")).count() + " lines set a control");
        line(out, "  so        every control is False: the worst case");
        line(out, "THE FINDINGS:");
        line(out, "  total     " + f.size() + ", from " + f.stream().map(ThreatTool.Finding::threat).distinct().count() + " of the " + library.size() + " threats");
        line(out, "THE FINDINGS BY SEVERITY:");
        for (String s : SEVERITY_ORDER) {
            line(out, String.format("  %-10s %3d", s, f.stream().filter(x -> x.severity().equals(s)).count()));
        }
        line(out, "THE FINDINGS PER ELEMENT (of the threats for its kind):");
        List<String> sum = new ArrayList<>();
        for (Design.Element e : bank.elements().stream().sorted((a, b) -> Long.compare(count(f, b.name()), count(f, a.name()))).toList()) {
            long n = count(f, e.name());
            sum.add(String.valueOf(n));
            line(out, String.format("  %-18s %3d of %-3d %s", e.name(), n, applying(library, e.kind().pytm), e.kind().pytm));
        }
        Set<Long> perFlow = bank.flows().stream().map(x -> count(f, x.label())).collect(Collectors.toCollection(TreeSet::new));
        if (perFlow.size() != 1) {
            throw new IllegalStateException("flows differ: " + perFlow);
        }
        long eachFlow = perFlow.iterator().next();
        line(out, String.format("  %-18s %3d of %-3d %s", bank.flows().size() + " flows, each", eachFlow, applying(library, "Dataflow"), "Dataflow"));
        line(out, "  sum   " + String.join(" + ", sum) + " + " + bank.flows().size() + " x " + eachFlow + " = " + f.size());
        line(out, "THE COMPARISON (the same bank):");
        line(out, "  plain STRIDE rules   " + PLAIN_STRIDE_CANDIDATES + " candidates");
        line(out, "  pytm                 " + f.size() + " findings, from the worst case");
        Map<String, Long> veryHigh = new LinkedHashMap<>();
        f.stream().filter(x -> x.severity().equals("Very High")).map(x -> x.threat() + "  " + x.title()).sorted().forEach(k -> veryHigh.merge(k, 1L, Long::sum));
        line(out, "THE VERY HIGH ONES (" + veryHigh.values().stream().mapToLong(Long::longValue).sum() + " findings from " + veryHigh.size() + " threats):");
        veryHigh.forEach((k, n) -> line(out, String.format("  %-6s %dx  %s", k.split("  ")[0], n, k.split("  ", 2)[1])));
    }

    private static long count(List<ThreatTool.Finding> f, String target) {
        return f.stream().filter(x -> x.target().equals(target)).count();
    }

    private static Set<String> keys(List<ThreatTool.Finding> f) {
        return f.stream().map(x -> x.threat() + "@" + x.target()).collect(Collectors.toCollection(TreeSet::new));
    }

    private static void act3(StringBuilder out, ThreatTool pytm, List<ThreatTool.Threat> library) throws Exception {
        line(out, "THREE. Every answer changes the findings.");
        Design bank = Design.bank();
        line(out, "THE STAGES (answers in the model, findings pytm reports):");
        line(out, String.format("  %-17s %7s %9s %5s %4s", "stage", "answers", "findings", "gone", "new"));
        Map<Answers.Stage, List<ThreatTool.Finding>> byStage = new LinkedHashMap<>();
        Set<String> previous = null;
        for (Answers.Stage stage : Answers.Stage.values()) {
            String python = model(bank, stage);
            List<ThreatTool.Finding> now = pytm.findings("bank-" + stage, python);
            byStage.put(stage, now);
            Set<String> after = keys(now);
            String gone = "-";
            String added = "-";
            if (previous != null) {
                Set<String> g = new TreeSet<>(previous);
                g.removeAll(after);
                Set<String> a = new TreeSet<>(after);
                a.removeAll(previous);
                gone = String.valueOf(g.size());
                added = String.valueOf(a.size());
            }
            line(out, String.format("  %-17s %7d %9d %5s %4s", stage, python.lines().filter(l -> l.contains(".controls.")).count(), now.size(), gone, added));
            previous = after;
        }
        ThreatTool.Threat ac06 = threat(library, "AC06");
        Design.Element gateway = bank.elements().stream().filter(e -> e.id().equals("gateway")).findFirst().orElseThrow();
        List<Condition.Term> terms = Condition.terms(ac06.condition());
        line(out, "ONE FINDING, WORKED OUT (" + ac06.id() + " on " + gateway.name() + "):");
        line(out, "  condition  " + brief(ac06.condition()));
        line(out, String.format("  %-17s %-11s %-17s %s", "stage", terms.get(0).answer(), terms.get(1).answer(), "reported"));
        for (Answers.Stage stage : Answers.Stage.values()) {
            Set<String> answers = Writer.trueAnswers(model(bank, stage), gateway.id());
            boolean byHand = Condition.holds(ac06.condition(), answers);
            boolean byPytm = byStage.get(stage).stream().anyMatch(x -> x.threat().equals(ac06.id()) && x.target().equals(gateway.name()));
            if (byHand != byPytm) {
                throw new IllegalStateException("the condition disagrees with pytm at " + stage);
            }
            line(out, String.format("  %-17s %-11s %-17s %s", stage, cap(answers.contains(terms.get(0).answer())), cap(answers.contains(terms.get(1).answer())), byHand ? "yes" : "no"));
        }
        line(out, "BEFORE THE ANSWER (" + Answers.Stage.ACCESS_AND_INPUT + "):");
        explain(out, ac06.condition(), Writer.trueAnswers(model(bank, Answers.Stage.ACCESS_AND_INPUT), gateway.id()));
        line(out, "AFTER THE ANSWER (" + Answers.Stage.RESILIENCE + "):");
        line(out, "  " + gateway.id() + ".controls." + terms.get(0).answer() + " = True");
        explain(out, ac06.condition(), Writer.trueAnswers(model(bank, Answers.Stage.RESILIENCE), gateway.id()));
        int checks = 0;
        int agree = 0;
        List<ThreatTool.Threat> simple = library.stream().filter(t -> Condition.simple(t.condition())).toList();
        for (Answers.Stage stage : Answers.Stage.values()) {
            String python = model(bank, stage);
            Set<String> found = keys(byStage.get(stage));
            for (Design.Element e : bank.elements()) {
                for (ThreatTool.Threat t : simple) {
                    if (t.targets().contains(e.kind().pytm)) {
                        checks++;
                        if (Condition.holds(t.condition(), Writer.trueAnswers(python, e.id())) == found.contains(t.id() + "@" + e.name())) {
                            agree++;
                        }
                    }
                }
            }
        }
        line(out, "THE CHECK (the class Condition redoes every simple one):");
        line(out, "  simple conditions  " + simple.size() + " of " + library.size() + " threats");
        line(out, "  checks             " + checks + " (element, threat, stage)");
        line(out, "  same as pytm       " + agree);
        line(out, "THE RESULT:");
        line(out, "  findings   " + byStage.get(Answers.Stage.NONE).size() + " -> " + byStage.get(Answers.Stage.RESILIENCE).size() + ": each answer removes some");
        line(out, "  but        an untrue answer hides a finding just the same");
        line(out, "  so         each answer needs an owner and a test");
    }

    private static String cap(boolean b) {
        return b ? "True" : "False";
    }

    /** Each test of a condition with its value, then the condition's value. */
    private static void explain(StringBuilder out, String condition, Set<String> answers) {
        if (condition.contains(" and ") || condition.contains("not ") || condition.contains("(")) {
            throw new IllegalArgumentException("only a list of or is worked out: " + condition);
        }
        List<String> values = new ArrayList<>();
        for (Condition.Term t : Condition.terms(condition)) {
            boolean v = t.holds(answers);
            values.add(cap(v));
            line(out, String.format("  %-27s -> %s", t.answer() + " is " + cap(t.value()), cap(v)));
        }
        boolean result = Condition.holds(condition, answers);
        line(out, "  " + String.join(" or ", values) + " = " + cap(result) + (result ? ": reported" : ": not reported"));
    }

    private static void act4(StringBuilder out, ThreatTool pytm) throws Exception {
        line(out, "FOUR. A diagram and a report.");
        String python = model(Design.bank(), Answers.Stage.RESILIENCE);
        line(out, "THE COMMANDS (one model, two outputs):");
        line(out, "  python3 bank.py --dfd > bank.dot");
        line(out, "  dot -Tsvg bank.dot -o bank.svg");
        line(out, "  python3 bank.py --report template.md > report.md");
        String dot = pytm.dfd("bank-dfd", python);
        List<String> clusters = dot.lines().filter(l -> l.contains("label = <<i>")).map(l -> l.replaceAll(".*<i>(.*)</i>.*", "$1")).toList();
        long nodes = dot.lines().filter(l -> l.matches("\\s+(actor|process|server|datastore|externalentity)_\\w+ \\[")).count();
        List<String> arrows = dot.lines().filter(l -> l.contains(" -> ")).toList();
        line(out, "THE DIAGRAM (graph text pytm wrote: " + dot.lines().count() + " lines):");
        line(out, "  boundaries  " + clusters.size() + ": " + String.join(", ", clusters));
        line(out, "  boxes       " + nodes + " (one per element)");
        line(out, "  arrows      " + arrows.size() + ", numbered (1) to (" + arrows.size() + ") in flow order");
        line(out, "  one arrow   " + arrows.get(1).trim().replaceAll("_[0-9a-f]{10}", "").replace(" [", ""));
        line(out, "THE TEMPLATE (ours: a heading, then a line per finding):");
        line(out, "  # {tm.name}");
        line(out, "  {findings:repeat:...}, each finding as:");
        line(out, "  * {{item.threat_id}} {{item.target}}: {{item.description}}");
        String report = pytm.report("bank-report", python, TEMPLATE);
        List<String> items = report.lines().filter(l -> l.startsWith("* ")).toList();
        line(out, "THE REPORT (Markdown, " + report.lines().count() + " lines):");
        line(out, "  " + report.lines().findFirst().orElseThrow());
        items.stream().limit(3).forEach(l -> line(out, "  " + l));
        line(out, "  ... " + items.size() + " lines in all, one per finding");
        line(out, "THE RESULT:");
        line(out, "  the findings, the diagram and the report: one model");
        line(out, "  so they cannot disagree with each other");
    }

    private static void act5(StringBuilder out, ThreatTool pytm, List<ThreatTool.Threat> library) throws Exception {
        line(out, "FIVE. The bill.");
        Design refunds = Design.withRefunds();
        Design.Flow f9 = refunds.flows().getLast();
        line(out, "THE CHANGE (refunds go to the card processor):");
        line(out, String.format("  %-2s  %-21s  %-10s  %s", f9.id(), f9.from() + " -> " + f9.to(), f9.protocol() + ":" + f9.port(), f9.label()));
        line(out, "  answers  none: f9 is in no list of Answers");
        List<ThreatTool.Finding> before = pytm.findings("bank-final", model(Design.bank(), Answers.Stage.RESILIENCE));
        List<ThreatTool.Finding> after = pytm.findings("bank-refunds", model(refunds, Answers.Stage.RESILIENCE));
        List<ThreatTool.Finding> onF9 = after.stream().filter(x -> x.target().equals(f9.label())).toList();
        line(out, "THE RESULT (the model regenerated from the design):");
        line(out, "  findings   " + before.size() + " before, " + after.size() + " after, " + onF9.size() + " on " + f9.label() + ":");
        onF9.forEach(x -> line(out, String.format("  %-6s %-7s %s", x.threat(), x.severity(), x.title())));
        List<ThreatTool.Finding> xml = before.stream().filter(x -> x.title().contains("XML")).toList();
        ThreatTool.Threat inp19 = threat(library, "INP19");
        Design.Element gateway = Design.bank().elements().stream().filter(e -> e.id().equals("gateway")).findFirst().orElseThrow();
        line(out, "THE XML FINDINGS, WORKED OUT (a bank that parses no XML):");
        line(out, "  findings   " + xml.size() + " mention XML");
        line(out, "  one        " + inp19.id() + " " + inp19.title() + ", " + xml.stream().filter(x -> x.threat().equals(inp19.id())).count() + "x");
        line(out, "  condition  " + brief(inp19.condition()));
        explain(out, inp19.condition(), Writer.trueAnswers(model(Design.bank(), Answers.Stage.RESILIENCE), gateway.id()));
        line(out, "  a person   reads it and says: does not apply");
        line(out, "WHAT pytm DOES NOT HOLD:");
        line(out, "  no owner, no accept or transfer, no review date");
        line(out, "  no check that a mitigation exists: the answers are claims");
        line(out, "  so the register of the plain project stays");
    }
}
