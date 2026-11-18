package com.jk.explore.security.threatmodel;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Each act prints its input and its result in
 * separate, labelled sections, and works out the values a reader would wonder about.
 */
public class ThreatModelDemo {

    /** The day the demo is run on, counted from the day the model was last reviewed. */
    static final int TODAY = 120;

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Model(out);
        act2Stride(out);
        act3Decisions(out);
        act4ThreatsAsTests(out);
        act5Habit(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Model(StringBuilder out) {
        line(out, "ONE. The system as a data-flow model.");
        Model m = BankModel.baseline();
        line(out, "  THE ELEMENTS, BY ZONE (trust 0 is the least):");
        m.elements().stream().sorted(Comparator.comparingInt((Element e) -> m.trust(e.zone()))).forEach(e -> line(out,
                String.format("    %-9s %d  %-3s%-19s %s", e.zone(), m.trust(e.zone()), e.id(), e.name(), e.kind().label + (e.publicFacing() ? ", public" : ""))));
        line(out, "  THE TRUST BOUNDARIES:");
        for (Boundary b : m.boundaries()) {
            line(out, String.format("    %-18s %s | %s", b.name(), b.zoneA(), b.zoneB()));
        }
        line(out, "  THE FLOWS, AND THE BOUNDARY EACH ONE CROSSES:");
        for (Flow f : m.flows()) {
            String crossed = m.boundaryOf(f).map(b -> b.name().replace(" boundary", "")).orElse("-");
            line(out, String.format("    %-3s %s -> %s  %-27s %s", f.id(), f.from(), f.to(), f.data(), crossed));
        }
        line(out, "  THE CHECK OF THE PICTURE:");
        line(out, "    the bank as built  " + m.problems().size() + " problems");
        Model draft = BankModel.draft();
        Element vault = draft.element("S3");
        Flow d1 = flow(draft, "D1");
        line(out, "  BEFORE THE CHECK: AN EARLIER DRAFT ADDS");
        line(out, String.format("    %-3s %s, a %s in %s, owner: %s", vault.id(), vault.name(), vault.kind().label, vault.zone(),
                vault.owner() == null ? "none" : vault.owner()));
        line(out, String.format("    %-3s %s -> %s  %s, from %s to %s", d1.id(), d1.from(), d1.to(), d1.data(), draft.zoneOf(d1.from()), draft.zoneOf(d1.to())));
        List<String> problems = draft.problems();
        line(out, "  AFTER THE CHECK: " + problems.size() + " PROBLEMS");
        problems.forEach(p -> line(out, "    " + p));
    }

    private static void act2Stride(StringBuilder out) {
        line(out, "TWO. STRIDE per element.");
        Model m = BankModel.baseline();
        Flow f2 = flow(m, "F2");
        line(out, "  SIX QUESTIONS ASKED OF ONE FLOW (" + f2.id() + " " + f2.data() + "):");
        StrideRules.explain(m, f2).forEach((s, why) -> line(out, String.format("    %s  %-12s %s", s.letter, shortName(s), why)));
        List<Threat> threats = StrideRules.generate(m);
        line(out, "  THE RULES ON THE WHOLE MODEL:");
        for (Stride s : Stride.values()) {
            List<String> targets = threats.stream().filter(t -> t.stride() == s).map(Threat::target).toList();
            line(out, String.format("    %s  %d  %-15s %s", s.letter, targets.size(), String.join(" ", targets), StrideRules.ASKED_OF.get(s)));
        }
        line(out, "    total " + threats.size() + " candidate threats");
        line(out, "  SIX OF THEM, ONE PER LETTER:");
        for (String key : List.of("S:E1", "T:F2", "R:F3", "I:S1", "D:P2", "E:F7")) {
            Threat t = threats.stream().filter(x -> x.key().equals(key)).findFirst().orElseThrow();
            line(out, "    " + t.key() + "  " + t.description());
        }
    }

    private static void act3Decisions(StringBuilder out) {
        line(out, "THREE. Rating and deciding.");
        Model m = BankModel.baseline();
        List<Threat> threats = StrideRules.generate(m);
        Flow f2 = flow(m, "F2");
        Rating r2 = Rating.of(m, new Threat(Stride.TAMPERING, "F2", ""));
        line(out, "  HOW ONE THREAT IS RATED (T:F2):");
        line(out, String.format("    likelihood  %d  the weaker end of F2 is the %s zone", r2.likelihood(), Rating.weakerZone(m, f2)));
        line(out, String.format("    impact      %d  F2 moves money (other flows 3)", r2.impact()));
        line(out, String.format("    risk        %d x %d = %d", r2.likelihood(), r2.impact(), r2.risk()));
        line(out, "  THE " + threats.size() + " THREATS ON THE RISK MATRIX (count per cell):");
        StringBuilder head = new StringBuilder(String.format("    %-19s", "likelihood \\ impact"));
        for (int i = 1; i <= 5; i++) {
            head.append(String.format("%4d", i));
        }
        line(out, head.toString());
        for (int l = 5; l >= 1; l--) {
            int like = l;
            String zones = Rating.LIKELIHOOD_BY_ZONE.entrySet().stream().filter(e -> e.getValue() == like).map(Map.Entry::getKey).sorted().collect(Collectors.joining(", "));
            StringBuilder row = new StringBuilder(String.format("    %d %-17s", l, zones.isEmpty() ? "" : "(" + zones + ")"));
            for (int i = 1; i <= 5; i++) {
                int impact = i;
                long n = threats.stream().map(t -> Rating.of(m, t)).filter(x -> x.likelihood() == like && x.impact() == impact).count();
                row.append(String.format("%4s", n == 0 ? "." : String.valueOf(n)));
            }
            line(out, row.toString());
        }
        List<Threat> top = threats.stream()
                .sorted(Comparator.comparingInt((Threat t) -> -Rating.of(m, t).risk()).thenComparing(Threat::key))
                .limit(3).toList();
        line(out, "  THE TOP THREE RISKS (ties in key order):");
        for (Threat t : top) {
            Rating r = Rating.of(m, t);
            line(out, String.format("    %-5s %d x %d = %d", t.key(), r.likelihood(), r.impact(), r.risk()));
        }
        Map<String, Decision> register = Register.bank();
        Map<Decision.Type, Integer> counts = new EnumMap<>(Decision.Type.class);
        threats.forEach(t -> counts.merge(register.get(t.key()).type(), 1, Integer::sum));
        Decision accepted = register.get("R:F6");
        line(out, "  THE DECISIONS (people decide, the rules do not):");
        line(out, String.format("    mitigate   %2d  each one names a check", counts.getOrDefault(Decision.Type.MITIGATE, 0)));
        line(out, String.format("    accept     %2d  R:F6  %s", counts.getOrDefault(Decision.Type.ACCEPT, 0), accepted.detail()));
        line(out, String.format("                     owner %s, review on day %d", accepted.owner(), accepted.reviewDay()));
        line(out, String.format("    transfer   %2d  D:P2  to %s", counts.getOrDefault(Decision.Type.TRANSFER, 0), register.get("D:P2").detail()));
        line(out, String.format("    avoid      %2d  I:S2  %s", counts.getOrDefault(Decision.Type.AVOID, 0), register.get("I:S2").detail()));
        line(out, String.format("    undecided  %2d", Register.undecided(threats, register).size()));
    }

    private static void act4ThreatsAsTests(StringBuilder out) {
        line(out, "FOUR. Mitigations as tests.");
        Model m = BankModel.baseline();
        Model changed = BankModel.withRefunds();
        Model draft = BankModel.draft();
        Map<String, Decision> register = Register.bank();
        int all = Checks.all().size();
        line(out, "  BEFORE THE CHANGE (the bank as built):");
        line(out, String.format("    checks failing     %d of %d", Checks.failures(m).size(), all));
        line(out, "    undecided threats  " + Register.undecided(StrideRules.generate(m), register).size());
        Flow f9 = flow(changed, "F9");
        line(out, "  THE CHANGE (refunds to the card processor):");
        line(out, String.format("    %s  %s -> %s  %s, crosses %s, encrypted: %s", f9.id(), f9.from(), f9.to(), f9.data(),
                changed.boundaryOf(f9).map(Boundary::name).orElse("-"), f9.encrypted() ? "yes" : "no"));
        Map<String, List<String>> failing = Checks.failures(changed);
        line(out, "  AFTER THE CHANGE:");
        line(out, String.format("    checks failing     %d of %d  <- FAILS BEFORE THE RELEASE", failing.size(), all));
        failing.forEach((name, broken) -> {
            line(out, "      " + name);
            broken.forEach(b -> line(out, "        " + b));
        });
        List<Threat> fresh = Register.undecided(StrideRules.generate(changed), register);
        line(out, "    undecided threats  " + fresh.size() + ": " + String.join(", ", fresh.stream().map(Threat::key).toList()));
        line(out, "  THE " + all + " CHECKS ON THREE MODELS:");
        line(out, String.format("    %-42s%6s%6s%7s", "", "built", "+F9", "draft"));
        Checks.all().forEach((name, check) -> line(out, String.format("    %-42s%6s%6s%7s", name, pass(check.apply(m)), pass(check.apply(changed)),
                pass(check.apply(draft)))));
        line(out, "  WHAT BREAKS IN THE EARLIER DRAFT:");
        Checks.failures(draft).values().forEach(broken -> broken.forEach(b -> line(out, "    " + b)));
    }

    private static void act5Habit(StringBuilder out) {
        line(out, "FIVE. The bill, and the habit.");
        Model m = BankModel.baseline();
        List<Threat> threats = StrideRules.generate(m);
        line(out, "  THE SIZE OF THE MODEL:");
        line(out, String.format("    %d elements, %d flows, %d boundaries, %d threats, %d checks", m.elements().size(), m.flows().size(),
                m.boundaries().size(), threats.size(), Checks.all().size()));
        Decision accepted = Register.bank().get("R:F6");
        int overdue = TODAY - accepted.reviewDay();
        line(out, "  THE ACCEPTED RISK R:F6:");
        line(out, "    accepted    day 0, review on day " + accepted.reviewDay());
        line(out, "    today       day " + TODAY);
        line(out, String.format("    overdue     %d - %d = %d days  <- THE DECISION HAS EXPIRED", TODAY, accepted.reviewDay(), overdue));
        line(out, "  WHEN TO OPEN THE MODEL AGAIN:");
        line(out, "    a new flow, store, boundary or third party; not once a year");
        line(out, "  THE FOUR QUESTIONS, AND THE PART THAT ANSWERS EACH:");
        line(out, "    what is being built?          the model (act ONE)");
        line(out, "    what can go wrong?            STRIDE (act TWO)");
        line(out, "    what is being done about it?  the decisions (act THREE)");
        line(out, "    was it done well enough?      the checks (act FOUR)");
    }

    private static Flow flow(Model m, String id) {
        return m.flows().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
    }

    private static String shortName(Stride s) {
        return s.label.toLowerCase(Locale.ROOT).replace("information ", "").replace(" of service", "").replace(" of privilege", "");
    }

    private static String pass(List<String> broken) {
        return broken.isEmpty() ? "pass" : "FAIL";
    }
}
