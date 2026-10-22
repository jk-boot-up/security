package com.jk.explore.security.pytmmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PytmDemo {

    static final List<String> SEVERITY_ORDER = List.of("Very High", "High", "Medium", "Low", "Very Low");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Pytm pytm = new Pytm(Path.of("build"));
        act1(out, pytm);
        act2(out, pytm);
        act3(out, pytm);
        act4(out, pytm);
        act5(out, pytm);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String model(Design design, Answers.Stage stage) {
        return Writer.python(design, stage, "Bank payments");
    }

    static int rank(Pytm.Finding f) {
        return SEVERITY_ORDER.indexOf(f.severity());
    }

    private static void act1(StringBuilder out, Pytm pytm) throws Exception {
        line(out, "ONE. The bank, written for pytm.");
        Design bank = Design.bank();
        String python = model(bank, Answers.Stage.NONE);
        line(out, "  the design: " + bank.elements().size() + " elements, " + bank.flows().size() + " flows and " + bank.boundaries().size() + " boundaries, written by the program as " + python.lines().count() + " lines of Python for pytm " + Pytm.VERSION);
        line(out, "  the first lines of the model:");
        python.lines().filter(l -> !l.isBlank()).limit(6).forEach(l -> line(out, "    " + l));
        line(out, "  pytm's library has " + pytm.libraryThreats(python) + " threats, each with a condition over the answers given about an element or a flow");
        List<Pytm.Finding> draft = pytm.findings("draft", model(Design.draft(), Answers.Stage.NONE));
        long forVault = draft.stream().filter(f -> f.target().equals("Card vault") || f.target().equals("Card number")).count();
        line(out, "  the draft of the plain project, with a card vault and a flow from the app that skips a boundary: pytm runs it without a complaint, and reports " + forVault + " findings about the new store and flow");
        line(out, "  the plain project's model check reported 2 problems for the same draft: pytm judges the answers about each element, not the shape of the picture");
    }

    private static void act2(StringBuilder out, Pytm pytm) throws Exception {
        line(out, "TWO. What pytm finds when nothing is said.");
        List<Pytm.Finding> f = pytm.findings("bank-none", model(Design.bank(), Answers.Stage.NONE));
        Map<String, Integer> severity = new TreeMap<>(Comparator.comparingInt(SEVERITY_ORDER::indexOf));
        f.forEach(x -> severity.merge(x.severity(), 1, Integer::sum));
        line(out, "  " + f.size() + " findings from " + f.stream().map(Pytm.Finding::threat).distinct().count() + " different threats; by severity: " + severity);
        Map<String, Long> perTarget = f.stream().collect(Collectors.groupingBy(Pytm.Finding::target, TreeMap::new, Collectors.counting()));
        line(out, "  per element or flow: " + perTarget);
        line(out, "  the most severe, in order:");
        f.stream().sorted(Comparator.comparingInt(PytmDemo::rank).thenComparing(Pytm.Finding::threat).thenComparing(Pytm.Finding::target)).limit(6)
                .forEach(x -> line(out, String.format("    %-9s %-6s %-20s %s", x.severity(), x.threat(), x.target(), x.title())));
        line(out, "  the plain project's STRIDE rules gave 20 candidates for the same bank; pytm checks each of its 113 threats against every element and flow it applies to, and starts from the worst case when nothing is said");
    }

    private static void act3(StringBuilder out, Pytm pytm) throws Exception {
        line(out, "THREE. Every answer changes the findings.");
        List<Pytm.Finding> previous = null;
        for (Answers.Stage stage : Answers.Stage.values()) {
            List<Pytm.Finding> now = pytm.findings("bank-" + stage, model(Design.bank(), stage));
            String change = "";
            if (previous != null) {
                TreeSet<String> before = previous.stream().map(x -> x.threat() + "@" + x.target()).collect(Collectors.toCollection(TreeSet::new));
                TreeSet<String> after = now.stream().map(x -> x.threat() + "@" + x.target()).collect(Collectors.toCollection(TreeSet::new));
                before.removeAll(after);
                change = ", " + before.size() + " gone";
            }
            line(out, String.format("  %-17s %3d findings%s  (%s)", stage, now.size(), change, stage.text()));
            previous = now;
        }
        line(out, "  the findings are a function of the answers: an answer that is not true makes a finding disappear, which is why each answer needs an owner and a test");
    }

    private static void act4(StringBuilder out, Pytm pytm) throws Exception {
        line(out, "FOUR. A diagram and a report.");
        String python = model(Design.bank(), Answers.Stage.RESILIENCE);
        String dot = pytm.dfd("bank-dfd", python);
        Path dotFile = pytm.work().resolve("bank.dot");
        Files.writeString(dotFile, dot);
        Path svgFile = pytm.work().resolve("bank.svg");
        Process p = new ProcessBuilder("dot", "-Tsvg", "-o", svgFile.toString(), dotFile.toString()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        p.waitFor();
        String svg = Files.readString(svgFile);
        line(out, "  --dfd writes a graph for Graphviz; rendered, the picture has " + count(svg, "class=\"node\"") + " nodes, " + count(svg, "class=\"edge\"") + " arrows and " + count(svg, "class=\"cluster\"") + " trust boundaries");
        Path template = pytm.work().resolve("report-template.md");
        Files.writeString(template, "# {tm.name}\n\n{tm.description}\n\n## Findings\n\n{findings:repeat:* {{item.id}} {{item.target}}: {{item.description}} ({{item.severity}})\n}\n");
        Pytm.Run report = pytm.model("bank-report", python, "--report", template.toString());
        long items = report.output().lines().filter(l -> l.startsWith("* ")).count();
        line(out, "  --report fills a template of our own: " + items + " findings listed, each with its element and severity, from " + report.output().lines().count() + " lines of Markdown");
        line(out, "  the diagram and the report come from the same model as the findings, so they cannot disagree with it");
    }

    private static long count(String text, String needle) {
        return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private static void act5(StringBuilder out, Pytm pytm) throws Exception {
        line(out, "FIVE. The bill.");
        List<Pytm.Finding> final0 = pytm.findings("bank-final", model(Design.bank(), Answers.Stage.RESILIENCE));
        List<Pytm.Finding> refunds = pytm.findings("bank-refunds", model(Design.withRefunds(), Answers.Stage.RESILIENCE));
        long onRefund = refunds.stream().filter(x -> x.target().equals("Refund")).count();
        line(out, "  a design change, refunds sent to the card processor over plain http: " + final0.size() + " findings become " + refunds.size() + ", " + onRefund + " of them about the new flow  <- THE MODEL CATCHES THE CHANGE ONLY IF THE MODEL IS CHANGED");
        long xml = final0.stream().filter(x -> x.title().toLowerCase().contains("xml")).count();
        line(out, "  findings that mention XML in a bank that parses none: " + xml + "; a library of generic threats gives candidates, and a person says which apply");
        line(out, "  pytm holds no decision: no owner, no accept or transfer, no review date, no check that a mitigation exists; those stay in the register of the plain project");
        line(out, "  the Python model is a second copy of the design: here it is written from the Java design so that there is one, and it is regenerated with every change");
        line(out, "  the answers are the team's claims about the system: a claim that is not tested is a finding that is hidden");
    }
}
