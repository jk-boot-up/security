package com.jk.explore.security.attacktree;

import java.util.List;

/** Runs the attack tree story in five numbered acts and prints exact, repeatable results. */
public class AttackTreeDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String money(double v) {
        return Double.isInfinite(v) ? "no way" : String.format("%,.0f EUR", v);
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Node tree = BankTree.baseline();

        out.append("== 1. The tree ==\n");
        print(out, tree, 0);

        out.append("\n== 2. What the tree says about the attacker ==\n");
        Analysis base = Analysis.of(tree);
        out.append("cheapest way in: ").append(money(base.minCost())).append(" via ").append(base.cheapestPath()).append('\n');
        out.append(String.format("chance that some way works: %.0f%%%n", base.probability() * 100));
        for (Node branch : ((Node.Gate) tree).children()) {
            Analysis a = Analysis.of(branch);
            out.append(String.format("  %-44s costs %-11s works %3.0f%%%n", branch.name(), money(a.minCost()), a.probability() * 100));
        }

        out.append("\n== 3. Each defence on its own ==\n");
        out.append(String.format("%-9s %-52s %10s %14s %8s%n", "defence", "what it is", "price", "cheapest way", "chance"));
        for (Planner.Step s : Planner.rank(tree, Defence.all())) {
            out.append(String.format("%-9s %-52s %10s %14s %7.0f%%%n", s.defence().id(), s.defence().name(), money(s.defence().price()), money(s.after().minCost()), s.after().probability() * 100));
        }

        out.append("\n== 4. A plan: always cut the cheapest path, then look again ==\n");
        out.append(String.format("%-4s %-9s %10s %14s %-24s %7s%n", "step", "defence", "spent", "cheapest way", "now via", "chance"));
        BankConfig config = BankConfig.weak();
        List<AbuseCase> cases = AbuseCase.all();
        out.append(String.format("%-4s %-9s %10s %14s %-24s %6.0f%%   abuse cases failing: %d of %d%n", 0, "(none)", money(0), money(base.minCost()), base.cheapestPath(),
                base.probability() * 100, failing(cases, config), cases.size()));
        int n = 0;
        for (Planner.Step s : Planner.plan(tree, Defence.all(), 5_000)) {
            config = config.with(s.defence().configChange());
            out.append(String.format("%-4d %-9s %10s %14s %-24s %6.0f%%   abuse cases failing: %d of %d%n", ++n, s.defence().id(), money(s.totalSpent()), money(s.after().minCost()),
                    s.after().cheapestPath(), s.after().probability() * 100, failing(cases, config), cases.size()));
        }

        out.append("\n== 5. Abuse cases as tests ==\n");
        BankConfig weak = BankConfig.weak();
        out.append("against the bank as it is:\n");
        for (AbuseCase c : cases) {
            out.append(String.format("  %-4s %-7s %s%n", c.id(), c.passes(weak) ? "passes" : "FAILS", c.story()));
        }
        out.append("after the plan above, the cases still failing:\n");
        for (AbuseCase c : cases) {
            if (!c.passes(config)) {
                out.append(String.format("  %-4s FAILS   %s%n", c.id(), c.story()));
            }
        }
        long leaves = countLeaves(tree);
        long covered = countCovered(tree);
        out.append(covered).append(" of ").append(leaves).append(" steps of the tree have an executable abuse case; the other ").append(leaves - covered).append(" is a judgement the tests cannot check\n");
        return out.toString();
    }

    static long failing(List<AbuseCase> cases, BankConfig c) {
        return cases.stream().filter(a -> !a.passes(c)).count();
    }

    static void print(StringBuilder out, Node n, int depth) {
        String indent = "  ".repeat(depth);
        switch (n) {
            case Node.Leaf l -> out.append(String.format("%s- [%s] %s  (%s, %.0f%%)%n", indent, l.id(), l.name(), money(l.cost()), l.probability() * 100));
            case Node.Gate g -> {
                out.append(indent).append(g.kind()).append(": ").append(g.name()).append('\n');
                g.children().forEach(c -> print(out, c, depth + 1));
            }
        }
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
}
