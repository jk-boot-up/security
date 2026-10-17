package com.jk.explore.security.threats;

import java.util.List;
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
                new Treatment("R1", Strategy.MITIGATE, "check the order owner on every read", 2, 1, 4),
                new Treatment("R2", Strategy.MITIGATE, "require a second approval for exports", 3, 1, 5),
                new Treatment("R3", Strategy.MITIGATE, "limit attempts and lock out after five", 3, 2, 3),
                new Treatment("R4", Strategy.AVOID, "delete the debug page", 1, 0, 0),
                new Treatment("R5", Strategy.ACCEPT, "moderate by hand", 0, 3, 2),
                new Treatment("R6", Strategy.TRANSFER, "use the payment provider's hosted page", 5, 1, 2),
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

    private static void act1Surface(StringBuilder out) {
        line(out, "ONE. The attack surface: every way in.");
        Surface surface = storeSurface();
        line(out, "  " + surface.summary());
        surface.add(new EntryPoint("POST /reviews", Exposure.AUTHENTICATED, "review text"));
        surface.add(new EntryPoint("GET /debug/config", Exposure.PUBLIC, "settings and keys"));
        line(out, "  the team ships reviews and leaves a debug page behind");
        line(out, "  " + surface.summary() + "  <- NEW WAYS IN");
    }

    private static void act2Vocabulary(StringBuilder out) {
        line(out, "TWO. Threat, vulnerability, asset, and risk.");
        Risk r1 = storeRisks().byId("R1");
        line(out, "  asset:         " + r1.asset());
        line(out, "  threat:        " + r1.threat());
        line(out, "  vulnerability: " + r1.vulnerability());
        line(out, "  likelihood " + r1.likelihood() + " x impact " + r1.impact() + " = score " + r1.score()
                + " (" + r1.rating() + ")");
    }

    private static void act3Ranking(StringBuilder out) {
        line(out, "THREE. Ranking the risks.");
        List<Risk> ranked = storeRisks().ranked();
        for (int i = 0; i < ranked.size(); i++) {
            Risk r = ranked.get(i);
            line(out, String.format("  %d. %s %2d %-6s %s", i + 1, r.id(), r.score(), r.rating(), r.threat()));
        }
        line(out, "  total score " + storeRisks().totalScore());
    }

    private static void act4Plan(StringBuilder out) {
        line(out, "FOUR. Spending a budget of " + BUDGET + " where it removes the most.");
        RiskRegister register = storeRisks();
        Plan plan = new Planner().plan(register, storeTreatments(), BUDGET);
        for (Treatment t : plan.funded()) {
            line(out, String.format("  funded   %s %-8s cost %d: %s", t.riskId(), t.strategy(), t.cost(), t.action()));
        }
        for (Treatment t : plan.unfunded()) {
            line(out, String.format("  waiting  %s %-8s cost %d: %s", t.riskId(), t.strategy(), t.cost(), t.action()));
        }
        line(out, "  spent " + plan.spent() + " of " + plan.budget());
        line(out, "  total score " + register.totalScore() + " -> " + plan.after().totalScore());
        Risk worst = plan.after().ranked().get(0);
        line(out, "  worst risk left: " + worst.id() + " " + worst.score() + " " + worst.rating());
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a score is a judgment, not a measurement.");
        RiskRegister base = storeRisks();
        RiskRegister engineering = base.rescored(Map.of("R3", new int[] {2, 3}, "R7", new int[] {4, 1}));
        RiskRegister finance = base.rescored(Map.of("R3", new int[] {5, 4}, "R7", new int[] {4, 3}));
        line(out, "  engineering top three: " + topThree(engineering));
        line(out, "  finance top three:     " + topThree(finance));
        line(out, "  same rules, different ranking  <- JUDGMENT");
        line(out, "  accepted risks need an owner and a review date: R5, R6, R7");
    }

    private static String topThree(RiskRegister register) {
        List<Risk> ranked = register.ranked();
        return ranked.get(0).id() + " (" + ranked.get(0).score() + "), " + ranked.get(1).id() + " ("
                + ranked.get(1).score() + "), " + ranked.get(2).id() + " (" + ranked.get(2).score() + ")";
    }
}
