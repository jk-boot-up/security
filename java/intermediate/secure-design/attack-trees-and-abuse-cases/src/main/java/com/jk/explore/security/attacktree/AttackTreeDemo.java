package com.jk.explore.security.attacktree;

import java.util.List;

/** Runs the attack tree story in five numbered acts and prints exact, repeatable results. */
public class AttackTreeDemo {

    /** The cheapest attack the plan aims for. */
    static final double TARGET = 5_000;

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String number(double v) {
        return Double.isInfinite(v) ? "no way" : String.format("%,.0f", v);
    }

    static String money(double v) {
        return Double.isInfinite(v) ? "no way" : number(v) + " EUR";
    }

    /** A chance as a decimal with at most three places: 0.9, 0.045. */
    static String decimal(double p) {
        return new java.math.BigDecimal(p).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    static String pct(double p) {
        return String.format("%.0f%%", p * 100);
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Node tree = BankTree.baseline();
        act1(out, tree);
        act2(out, tree);
        act3(out, tree);
        BankConfig planned = act4(out, tree);
        act5(out, tree, planned);
        return out.toString();
    }

    private static void act1(StringBuilder out, Node tree) {
        line(out, "ONE. The tree.");
        Node.Gate root = (Node.Gate) tree;
        line(out, "THE GOAL (the root of the tree):");
        line(out, "  " + root.kind() + "  " + root.name());
        line(out, "THE FIRST BRANCH (cost to the attacker, chance it works):");
        print(out, root.children().getFirst(), 1);
        line(out, "THE OTHER BRANCHES:");
        root.children().stream().skip(1).forEach(c -> print(out, c, 1));
        line(out, "THE RULES (how the tree is read):");
        line(out, "  OR   any child will do: the cheapest child wins");
        line(out, "       chance 1 - (1 - p1)(1 - p2)..., some child works");
        line(out, "  AND  every child is needed: the costs add up");
        line(out, "       chance p1 x p2 x ..., every child works");
    }

    static void print(StringBuilder out, Node n, int depth) {
        String indent = "  ".repeat(depth);
        switch (n) {
            case Node.Leaf l -> line(out, String.format("%s%-4s %9s %4s  %s", indent, l.id(), money(l.cost()), pct(l.probability()), l.name()));
            case Node.Gate g -> {
                line(out, indent + g.kind() + "  " + g.name());
                g.children().forEach(c -> print(out, c, depth + 1));
            }
        }
    }

    static Node.Gate gate(Node n, String name) {
        if (n instanceof Node.Gate g) {
            if (g.name().equals(name)) {
                return g;
            }
            for (Node c : g.children()) {
                Node.Gate found = gate(c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void act2(StringBuilder out, Node tree) {
        line(out, "TWO. What the tree says about the attacker.");
        Node.Gate phish = gate(tree, "Phish her password and one-time code");
        GateSteps p = GateSteps.of(phish);
        line(out, "AN AND GATE, WORKED OUT (" + phish.name() + "):");
        line(out, "  cost     " + p.cost() + " EUR");
        line(out, "  chance   " + p.chance());
        Node.Gate login = gate(tree, "Log in as Ana");
        GateSteps l = GateSteps.of(login);
        line(out, "AN OR GATE, WORKED OUT (" + login.name() + "):");
        line(out, "  the AND gates under it:");
        for (Node c : login.children()) {
            Analysis a = Analysis.of(c);
            line(out, String.format("    %-38s %9s %4s", c.name(), money(a.minCost()), pct(a.probability())));
        }
        line(out, "  cost     " + l.cost() + " EUR");
        line(out, "  chance   " + l.chance().substring(0, l.chance().lastIndexOf(" = ")));
        line(out, "           = " + l.chance().substring(l.chance().lastIndexOf("= ") + 2));
        Node.Gate root = (Node.Gate) tree;
        Analysis base = Analysis.of(tree);
        line(out, "THE ROOT, AN OR OF FOUR BRANCHES:");
        for (Node c : root.children()) {
            Analysis a = Analysis.of(c);
            line(out, String.format("  %-41s %10s %4s", c.name(), money(a.minCost()), pct(a.probability())));
        }
        line(out, "THE RESULT:");
        line(out, "  cheapest way in   " + money(base.minCost()) + " via " + base.cheapestPath());
        line(out, "  some way works    " + pct(base.probability()));
    }

    private static void act3(StringBuilder out, Node tree) {
        line(out, "THREE. Each defence on its own.");
        line(out, "THE DEFENCES (price, what it is):");
        for (Defence d : Defence.all()) {
            line(out, String.format("  %-8s %10s  %s", d.id(), money(d.price()), d.name()));
        }
        line(out, "EACH ALONE, ON THE TREE (cheapest way, chance):");
        for (Planner.Step s : Planner.rank(tree, Defence.all())) {
            line(out, String.format("  %-8s %10s  %-16s %4s", s.defence().id(), money(s.after().minCost()), s.after().cheapestPath(), pct(s.after().probability())));
        }
        Defence own = Defence.all().getFirst();
        Node.Gate transfer = gate(tree, "Abuse the transfer call");
        Analysis before = Analysis.of(transfer);
        Analysis after = Analysis.of(own.applyTo(transfer));
        line(out, "BEFORE THE DEFENCE (" + transfer.name() + "):");
        line(out, "  cheapest  " + money(before.minCost()) + " via " + before.cheapestPath() + ", chance " + pct(before.probability()));
        line(out, "AFTER THE DEFENCE (" + own.id() + " removes " + String.join(", ", own.effects().keySet()) + "):");
        GateSteps forged = GateSteps.of(gate(own.applyTo(transfer), "Forge a request from another site"));
        line(out, "  left      the forged request: " + forged.cost() + " EUR");
        line(out, "  cheapest  " + money(after.minCost()) + " via " + after.cheapestPath() + ", chance " + pct(after.probability()));
        Analysis passkey = Analysis.of(Defence.all().stream().filter(d -> d.id().equals("PASSKEY")).findFirst().orElseThrow().applyTo(tree));
        line(out, "THE RESULT:");
        Planner.Step best = Planner.rank(tree, Defence.all()).getFirst();
        line(out, "  the best one alone  " + best.defence().id() + ", " + money(best.defence().price()) + ": the attacker pays " + money(best.after().minCost()));
        line(out, "  PASSKEY alone       " + money(Defence.all().get(4).price()) + ", still " + money(passkey.minCost()) + " via " + passkey.cheapestPath());
    }

    private static BankConfig act4(StringBuilder out, Node tree) {
        line(out, "FOUR. A plan: cut the cheapest path, then look again.");
        Analysis base = Analysis.of(tree);
        List<AbuseCase> cases = AbuseCase.all();
        BankConfig config = BankConfig.weak();
        line(out, "THE TARGET:");
        line(out, "  the cheapest attack costs at least " + money(TARGET));
        line(out, "THE STEPS (chosen by what each does to the tree):");
        line(out, String.format("  %-4s %-8s %10s  %10s  %-8s %4s  %s", "step", "defence", "spent", "cheapest", "via", "p", "fail"));
        line(out, String.format("  %-4s %-8s %10s  %10s  %-8s %4s  %s", 0, "(none)", money(0), money(base.minCost()), String.join(",", base.cheapestPath()),
                pct(base.probability()), failing(cases, config) + "/" + cases.size()));
        int n = 0;
        Planner.Step last = null;
        for (Planner.Step s : Planner.plan(tree, Defence.all(), TARGET)) {
            config = config.with(s.defence().configChange());
            line(out, String.format("  %-4d %-8s %10s  %10s  %-8s %4s  %s", ++n, s.defence().id(), money(s.totalSpent()), money(s.after().minCost()),
                    String.join(",", s.after().cheapestPath()), pct(s.after().probability()), failing(cases, config) + "/" + cases.size()));
            last = s;
        }
        line(out, "  (p: the chance some way works; fail: abuse cases failing)");
        line(out, "THE RESULT:");
        line(out, "  spent       " + money(last.totalSpent()) + " on " + n + " of " + Defence.all().size() + " defences");
        line(out, "  cheapest    " + money(base.minCost()) + " -> " + money(last.after().minCost()));
        line(out, "  chance      " + pct(base.probability()) + " -> " + pct(last.after().probability()));
        return config;
    }

    private static void act5(StringBuilder out, Node tree, BankConfig planned) {
        line(out, "FIVE. Abuse cases as tests.");
        List<AbuseCase> cases = AbuseCase.all();
        BankConfig weak = BankConfig.weak();
        AbuseCase ac5 = cases.get(4);
        line(out, "ONE ABUSE CASE (the test of leaf B1):");
        line(out, "  " + ac5.id() + "     " + ac5.story());
        line(out, "  passes  when the bank refuses it: ownership is on");
        line(out, "BEFORE THE PLAN (the bank as it is, nothing on):");
        for (AbuseCase c : cases) {
            line(out, String.format("  %-4s %-7s %s", c.id(), c.passes(weak) ? "PASSES" : "FAILS", c.story()));
        }
        line(out, "AFTER THE PLAN (on: " + String.join(", ", planned.enabled().stream().sorted().toList()) + "):");
        for (AbuseCase c : cases) {
            line(out, String.format("  %-4s %-7s %s", c.id(), c.passes(planned) ? "PASSES" : "FAILS", c.story()));
        }
        long leaves = countLeaves(tree);
        long covered = countCovered(tree);
        line(out, "THE RESULT:");
        line(out, "  steps with a test   " + covered + " of " + leaves);
        line(out, "  without one         " + (leaves - covered) + ": " + uncovered(tree) + ", a judgement no test checks");
        line(out, "  still failing       " + failing(cases, planned) + ": the risks the plan accepts, in writing");
    }

    static long failing(List<AbuseCase> cases, BankConfig c) {
        return cases.stream().filter(a -> !a.passes(c)).count();
    }

    static long countLeaves(Node n) {
        return switch (n) {
            case Node.Leaf l -> 1;
            case Node.Gate g -> g.children().stream().mapToLong(AttackTreeDemo::countLeaves).sum();
        };
    }

    static long countCovered(Node n) {
        return switch (n) {
            case Node.Leaf l -> l.abuseCase().isEmpty() ? 0 : 1;
            case Node.Gate g -> g.children().stream().mapToLong(AttackTreeDemo::countCovered).sum();
        };
    }

    static String uncovered(Node n) {
        return switch (n) {
            case Node.Leaf l -> l.abuseCase().isEmpty() ? l.id() : "";
            case Node.Gate g -> String.join("", g.children().stream().map(AttackTreeDemo::uncovered).toList());
        };
    }
}
