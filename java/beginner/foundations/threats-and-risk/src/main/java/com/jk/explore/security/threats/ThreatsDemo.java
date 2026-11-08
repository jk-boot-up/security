package com.jk.explore.security.threats;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class ThreatsDemo {

    static final int BUDGET = 10;

    static Surface storeSurface() {
        return new Surface()
                .add(new EntryPoint("GET /products", Exposure.PUBLIC, "the catalogue"))
                .add(new EntryPoint("POST /login", Exposure.PUBLIC, "passwords"))
                .add(new EntryPoint("POST /checkout", Exposure.AUTHENTICATED, "card details"))
                .add(new EntryPoint("GET /orders/{id}", Exposure.AUTHENTICATED, "customer orders"))
                .add(new EntryPoint("POST /admin/refund", Exposure.INTERNAL, "money"))
                .add(new EntryPoint("GET /admin/export", Exposure.INTERNAL, "all customer data"));
    }

    static RiskRegister storeRisks() {
        return new RiskRegister(List.of(
                new Risk("R1", "Customer order data stolen", "/orders/{id} does not check the owner", "customer orders", 4, 4),
                new Risk("R2", "Customer export abused", "the export has no second approval", "customer data", 2, 5),
                new Risk("R3", "Stolen passwords tried on logins", "/login has no limit on attempts", "customer accounts", 4, 3),
                new Risk("R4", "Debug page leaks secrets", "/debug/config is open to everyone", "passwords and keys", 3, 5),
                new Risk("R5", "Fake reviews flood the shop", "/reviews has no spam check", "trust in the shop", 3, 2),
                new Risk("R6", "Card details read in transit", "checkout accepts plain HTTP in one setting", "card details", 2, 5),
                new Risk("R7", "Price list scraped by rivals", "/products has no limits", "the price list", 4, 1)));
    }

    static List<Treatment> storeTreatments() {
        return List.of(
                new Treatment("R1", Strategy.MITIGATE, "check the order owner", 2, 1, 4),
                new Treatment("R2", Strategy.MITIGATE, "second approval on exports", 3, 1, 5),
                new Treatment("R3", Strategy.MITIGATE, "limit login attempts", 3, 2, 3),
                new Treatment("R4", Strategy.AVOID, "delete the debug page", 1, 0, 0),
                new Treatment("R5", Strategy.ACCEPT, "moderate by hand", 0, 3, 2),
                new Treatment("R6", Strategy.TRANSFER, "provider's hosted page", 5, 1, 2),
                new Treatment("R7", Strategy.ACCEPT, "live with it", 0, 4, 1));
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Surface(out);
        act2Vocabulary(out);
        act3Ranking(out);
        act4Plan(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String entry(EntryPoint e) {
        return String.format("%-13s  %-18s  %s", e.exposure().name().toLowerCase(), e.route(), e.data());
    }

    private static void act1Surface(StringBuilder out) {
        line(out, "ONE. The attack surface: every way in.");
        Surface surface = storeSurface();
        List<EntryPoint> before = surface.entryPoints();
        line(out, "  BEFORE THE RELEASE (" + surface.size() + " entry points):");
        for (EntryPoint e : before) {
            line(out, "    " + entry(e));
        }
        line(out, "    " + surface.summary());
        long publicBefore = surface.count(Exposure.PUBLIC);
        surface.add(new EntryPoint("POST /reviews", Exposure.AUTHENTICATED, "review text"));
        surface.add(new EntryPoint("GET /debug/config", Exposure.PUBLIC, "settings, keys"));
        line(out, "  AFTER THE RELEASE (reviews added, debug page left):");
        for (EntryPoint e : surface.entryPoints()) {
            if (!before.contains(e)) {
                line(out, "    + " + entry(e));
            }
        }
        line(out, "    " + surface.summary());
        line(out, "  " + (surface.size() - before.size()) + " new ways in, "
                + (surface.count(Exposure.PUBLIC) - publicBefore) + " open to everyone  <- COUNT AGAIN");
    }

    private static void act2Vocabulary(StringBuilder out) {
        line(out, "TWO. Threat, vulnerability, asset, and risk.");
        Risk r1 = storeRisks().byId("R1");
        line(out, "  THE RISK (" + r1.id() + "), IN FOUR WORDS:");
        line(out, "    asset          " + r1.asset());
        line(out, "    threat         " + r1.threat());
        line(out, "    vulnerability  " + r1.vulnerability());
        line(out, "    risk           how likely, and how bad");
        line(out, "  THE SCORE, STEP BY STEP:");
        line(out, "    step 1  likelihood  " + r1.likelihood() + " of 5");
        line(out, "    step 2  impact      " + r1.impact() + " of 5");
        line(out, "    step 3  score       " + r1.likelihood() + " x " + r1.impact() + " = " + r1.score());
        line(out, "    step 4  rating      " + r1.score() + " is in " + r1.rating().range() + ", so " + r1.rating());
        line(out, "  bands: LOW " + Rating.LOW.range() + ", MEDIUM " + Rating.MEDIUM.range()
                + ", HIGH " + Rating.HIGH.range());
    }

    private static void act3Ranking(StringBuilder out) {
        line(out, "THREE. Ranking the risks.");
        RiskRegister register = storeRisks();
        line(out, "  BEFORE RANKING (in the order they were written):");
        for (Risk r : register.all()) {
            line(out, String.format("    %s  %d x %d = %2d  %s", r.id(), r.likelihood(), r.impact(), r.score(), r.threat()));
        }
        List<Risk> ranked = register.ranked();
        line(out, "  AFTER RANKING (highest score first):");
        for (int i = 0; i < ranked.size(); i++) {
            Risk r = ranked.get(i);
            line(out, String.format("    %d. %s %2d %-6s %s", i + 1, r.id(), r.score(), r.rating(), r.threat()));
        }
        for (int i = 1; i < ranked.size(); i++) {
            Risk a = ranked.get(i - 1);
            Risk b = ranked.get(i);
            if (a.score() == b.score()) {
                String rule = a.impact() != b.impact() ? "higher impact first" : "same impact, so lower id first";
                line(out, "  tie: " + a.id() + " and " + b.id() + " both " + a.score() + ", " + rule);
            }
        }
        line(out, "  total score " + register.totalScore());
    }

    private static void act4Plan(StringBuilder out) {
        line(out, "FOUR. Spending a budget of " + BUDGET + " where it removes the most.");
        RiskRegister register = storeRisks();
        List<Treatment> options = storeTreatments();
        line(out, "  BEFORE THE PLAN (value = score removed / cost):");
        for (Treatment t : Planner.byValue(register, options)) {
            int now = register.byId(t.riskId()).score();
            line(out, String.format(Locale.ROOT, "    %s %-8s  %2d -> %d  removes %2d / cost %d = %4.1f", t.riskId(), t.strategy(),
                    now, t.newLikelihood() * t.newImpact(), Planner.reduction(register, t), t.cost(),
                    Planner.value(register, t)));
        }
        Plan plan = new Planner().plan(register, options, BUDGET);
        line(out, "  AFTER THE PLAN (best value first, within budget):");
        int spent = 0;
        for (Treatment t : plan.funded()) {
            spent += t.cost();
            line(out, String.format("    funded   %s cost %d, spent %d: %s", t.riskId(), t.cost(), spent, t.action()));
        }
        for (Treatment t : plan.unfunded()) {
            line(out, String.format("    waiting  %s cost %d, %d + %d > %d: %s", t.riskId(), t.cost(), plan.spent(),
                    t.cost(), plan.budget(), t.action()));
        }
        line(out, "    spent " + plan.spent() + " of " + plan.budget());
        line(out, "    total score " + register.totalScore() + " -> " + plan.after().totalScore());
        Risk worst = plan.after().ranked().get(0);
        line(out, "    worst risk left: " + worst.id() + " " + worst.score() + " " + worst.rating());
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a score is a judgment, not a measurement.");
        RiskRegister base = storeRisks();
        Map<String, int[]> engineeringView = Map.of("R3", new int[] {2, 3}, "R7", new int[] {4, 1});
        Map<String, int[]> financeView = Map.of("R3", new int[] {5, 4}, "R7", new int[] {4, 3});
        RiskRegister engineering = base.rescored(engineeringView);
        RiskRegister finance = base.rescored(financeView);
        line(out, "  THE TWO REVIEWS (same rules, different estimates):");
        for (String id : List.of("R3", "R7")) {
            Risk e = engineering.byId(id);
            Risk f = finance.byId(id);
            line(out, String.format("    %s  engineering %d x %d = %2d   finance %d x %d = %2d", id,
                    e.likelihood(), e.impact(), e.score(), f.likelihood(), f.impact(), f.score()));
        }
        line(out, "  THE TWO RANKINGS:");
        line(out, "    engineering top three: " + topThree(engineering));
        line(out, "    finance top three:     " + topThree(finance));
        line(out, "  same rules, different ranking  <- JUDGMENT");
        Plan plan = new Planner().plan(base, storeTreatments(), BUDGET);
        List<String> funded = plan.funded().stream().map(Treatment::riskId).toList();
        List<String> accepted = base.all().stream().map(Risk::id).filter(id -> !funded.contains(id)).toList();
        line(out, "  accepted " + String.join(", ", accepted) + ": each needs an owner and a date");
    }

    private static String topThree(RiskRegister register) {
        List<Risk> ranked = register.ranked();
        return ranked.get(0).id() + " (" + ranked.get(0).score() + "), " + ranked.get(1).id() + " ("
                + ranked.get(1).score() + "), " + ranked.get(2).id() + " (" + ranked.get(2).score() + ")";
    }
}
