package com.jk.explore.security.threatmodel;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
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
        long externals = m.elements().stream().filter(e -> e.kind() == Kind.EXTERNAL_ENTITY).count();
        long processes = m.elements().stream().filter(e -> e.kind() == Kind.PROCESS).count();
        long stores = m.elements().stream().filter(e -> e.kind() == Kind.DATA_STORE).count();
        line(out, "  the bank: " + externals + " external entities, " + processes + " processes, " + stores + " data stores, "
                + m.flows().size() + " flows and " + m.boundaries().size() + " trust boundaries");
        for (Boundary b : m.boundaries()) {
            List<String> crossing = m.flows().stream().filter(f -> m.boundaryOf(f).filter(b::equals).isPresent()).map(Flow::id).toList();
            line(out, "  " + b.name() + " (" + b.zoneA() + " | " + b.zoneB() + "): crossed by " + String.join(", ", crossing));
        }
        line(out, "  problems in the model: " + m.problems().size());
        Model draft = BankModel.draft();
        List<String> problems = draft.problems();
        line(out, "  an earlier draft, where the app writes card numbers straight into a card vault: " + problems.size() + " problems");
        for (String p : problems) {
            line(out, "    " + p);
        }
        line(out, "  a picture that skips a boundary hides the threats that live on it, so the model is checked before it is used");
    }

    private static void act2Stride(StringBuilder out) {
        line(out, "TWO. STRIDE per element: questions nobody forgets to ask.");
        Model m = BankModel.baseline();
        List<Threat> threats = StrideRules.generate(m);
        Map<Stride, Integer> counts = new EnumMap<>(Stride.class);
        threats.forEach(t -> counts.merge(t.stride(), 1, Integer::sum));
        StringBuilder byCategory = new StringBuilder();
        for (Stride s : Stride.values()) {
            byCategory.append(byCategory.isEmpty() ? "" : ", ").append(s.letter).append(' ').append(counts.getOrDefault(s, 0));
        }
        line(out, "  the rules find " + threats.size() + " candidate threats: " + byCategory);
        for (String key : List.of("S:E1", "T:F2", "R:F3", "I:S1", "D:P2", "E:F7")) {
            Threat t = threats.stream().filter(x -> x.key().equals(key)).findFirst().orElseThrow();
            line(out, "  TH-" + number(threats, t) + " " + t.key() + " " + t.stride().label.toLowerCase(Locale.ROOT) + ": " + t.description());
        }
        line(out, "  each category is the opposite of a property: spoofing against authenticity, tampering against integrity, and so on");
    }

    private static String number(List<Threat> threats, Threat t) {
        return String.format(Locale.ROOT, "%02d", threats.indexOf(t) + 1);
    }

    private static void act3Decisions(StringBuilder out) {
        line(out, "THREE. Rating and deciding.");
        Model m = BankModel.baseline();
        List<Threat> threats = StrideRules.generate(m);
        List<Threat> top = threats.stream()
                .sorted(Comparator.comparingInt((Threat t) -> -Rating.of(m, t).risk()).thenComparing(Threat::key))
                .limit(3).toList();
        for (Threat t : top) {
            Rating r = Rating.of(m, t);
            line(out, "  highest risk: " + t.key() + " likelihood " + r.likelihood() + " x impact " + r.impact() + " = " + r.risk()
                    + " (" + t.description() + ")");
        }
        Map<String, Decision> register = Register.bank();
        Map<Decision.Type, Integer> counts = new EnumMap<>(Decision.Type.class);
        threats.forEach(t -> counts.merge(register.get(t.key()).type(), 1, Integer::sum));
        line(out, "  decisions: mitigate " + counts.getOrDefault(Decision.Type.MITIGATE, 0)
                + ", accept " + counts.getOrDefault(Decision.Type.ACCEPT, 0)
                + ", transfer " + counts.getOrDefault(Decision.Type.TRANSFER, 0)
                + ", avoid " + counts.getOrDefault(Decision.Type.AVOID, 0));
        Decision accepted = register.get("R:F6");
        line(out, "  accepted R:F6, " + accepted.detail() + "; owner " + accepted.owner() + ", review on day " + accepted.reviewDay());
        line(out, "  transferred D:P2 to " + register.get("D:P2").detail());
        line(out, "  avoided I:S2: " + register.get("I:S2").detail());
        line(out, "  undecided threats: " + Register.undecided(threats, register).size());
    }

    private static void act4ThreatsAsTests(StringBuilder out) {
        line(out, "FOUR. Mitigations as tests.");
        Model m = BankModel.baseline();
        Map<String, Decision> register = Register.bank();
        line(out, "  " + Checks.all().size() + " checks; every mitigation names one: " + Register.unproven(register).isEmpty());
        line(out, "  checks failing on the bank as built: " + Checks.failures(m).size());
        Model changed = BankModel.withRefunds();
        line(out, "  a design change adds F9: the payments service sends refunds to the card processor, unencrypted");
        Map<String, List<String>> failing = Checks.failures(changed);
        line(out, "  checks failing now: " + failing.size());
        failing.forEach((name, broken) -> line(out, "    " + name + ": " + String.join("; ", broken) + "  <- THE TEST FAILS BEFORE THE RELEASE"));
        List<Threat> fresh = Register.undecided(StrideRules.generate(changed), register);
        line(out, "  new threats with no decision: " + fresh.size() + " ("
                + String.join(", ", fresh.stream().map(Threat::key).toList()) + ")");
        Model draft = BankModel.draft();
        line(out, "  the earlier draft fails " + Checks.failures(draft).size() + " checks, so it would never have shipped either");
    }

    private static void act5Habit(StringBuilder out) {
        line(out, "FIVE. The bill, and the habit.");
        Model m = BankModel.baseline();
        List<Threat> threats = StrideRules.generate(m);
        line(out, "  the model is " + m.elements().size() + " elements, " + m.flows().size() + " flows and " + threats.size()
                + " threats: small enough to read in one sitting, and to keep up to date");
        Decision accepted = Register.bank().get("R:F6");
        boolean overdue = TODAY > accepted.reviewDay();
        line(out, "  today is day " + TODAY + "; accepted risks past their review day: " + (overdue ? 1 : 0)
                + "  <- AN ACCEPTED RISK IS A DECISION WITH AN EXPIRY DATE");
        line(out, "  review the model when a flow, a store, a boundary or a third party is added, not once a year");
        line(out, "  four questions, every time: what is being built? what can go wrong? what is being done about it? was it done well enough?");
        line(out, "  the rules only find candidates: people still judge likelihood, impact and the right control");
    }
}
