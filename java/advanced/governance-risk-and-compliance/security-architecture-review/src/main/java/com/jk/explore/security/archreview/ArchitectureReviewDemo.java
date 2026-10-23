package com.jk.explore.security.archreview;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ArchitectureReviewDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String score(List<Planted> planted, List<Finding> findings) {
        long found = planted.stream().filter(p -> p.rule() != null && p.reportedBy(findings)).count();
        long extra = findings.stream().filter(f -> planted.stream().noneMatch(p -> f.rule().equals(p.rule()) && f.subject().equals(p.subject()))).count();
        return found + " of " + planted.size() + " planted weaknesses reported, " + extra + " other findings";
    }

    static String names(List<Planted> ps) {
        return String.join("; ", ps.stream().map(Planted::description).toList());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Design v1 = Design.load(Path.of("design/v1/bank.dsl"));
        Map<String, Adr> adrs1 = Adr.load(Path.of("design/v1/adr"));
        long people = v1.parts().stream().filter(p -> p.kind() == Design.Kind.PERSON).count();
        long stores = v1.parts().stream().filter(p -> p.kind() == Design.Kind.DATASTORE).count();

        out.append("--- the design as drawn\n");
        out.append("parts: " + v1.parts().size() + " (" + people + " people, 1 external system, " + stores + " data stores, " + (v1.parts().size() - people - stores - 1) + " other containers), flows: " + v1.flows().size() + ", zones: " + v1.parts().stream().map(Design.Part::zone).distinct().count() + "\n");
        out.append("flows that cross a zone: " + v1.flows().stream().filter(Design.Flow::crossesZones).count() + "\n");
        out.append("decision records: " + adrs1.size() + ", accepted " + adrs1.values().stream().filter(Adr::accepted).count() + "\n");
        out.append("STRIDE questions a review would ask: " + Rules.strideQuestions(v1) + "\n");

        out.append("--- rules over the design\n");
        List<Finding> first = Rules.run(v1, adrs1, Rules.FIRST_SET);
        out.append("planted weaknesses: " + Planted.FIRST_DESIGN.size() + "\n");
        out.append("first " + Rules.FIRST_SET.size() + " rules: " + score(Planted.FIRST_DESIGN, first) + "\n");
        for (Finding f : first) {
            out.append("  " + f.rule() + ": " + f.subject() + "\n");
        }
        out.append("not reported: " + names(Planted.FIRST_DESIGN.stream().filter(p -> p.rule() == null || !p.reportedBy(first)).toList()) + "\n");

        out.append("--- one more rule\n");
        List<Finding> all = Rules.run(v1, adrs1, Rules.ALL);
        out.append("with a rule that wants every flow encrypted and authenticated: " + score(Planted.FIRST_DESIGN, all) + "\n");
        out.append("not reported: " + names(Planted.FIRST_DESIGN.stream().filter(p -> p.rule() == null || !p.reportedBy(all)).toList()) + "\n");

        out.append("--- the reviewed design\n");
        Design v2 = Design.load(Path.of("design/v2/bank.dsl"));
        Map<String, Adr> adrs2 = Adr.load(Path.of("design/v2/adr"));
        List<Finding> after = Rules.run(v2, adrs2, Rules.ALL);
        out.append("parts: " + v2.parts().size() + ", flows: " + v2.flows().size() + ", flows that cross a zone: " + v2.flows().stream().filter(Design.Flow::crossesZones).count() + "\n");
        out.append("decision records: " + adrs2.size() + ", accepted " + adrs2.values().stream().filter(Adr::accepted).count() + "\n");
        out.append("findings from all " + Rules.ALL.size() + " rules: " + after.size() + "\n");
        for (Finding f : after) {
            out.append("  " + f.rule() + ": " + f.subject() + "\n");
        }
        out.append("paths from the web zone to confidential data that skip the application zone: " + v2.flows().stream().filter(f -> f.to().prop("classification").equals("confidential") && f.from().zone().equals("dmz")).count() + " (before: " + v1.flows().stream().filter(f -> f.to().prop("classification").equals("confidential") && f.from().zone().equals("dmz")).count() + ")\n");

        out.append("--- the bill\n");
        out.append("rules read the model, and the model is what the team drew: a flaw that is not in the picture is not found\n");
        out.append("the hidden form field needs a person who asks what the web application trusts\n");
        out.append("a design with no findings is a design the rules agree with, not a safe one\n");
        return out.toString();
    }
}
