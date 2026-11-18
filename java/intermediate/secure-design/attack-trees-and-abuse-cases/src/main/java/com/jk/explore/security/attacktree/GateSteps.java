package com.jk.explore.security.attacktree;

import java.util.ArrayList;
import java.util.List;

/**
 * A teaching class: works out one gate by hand, with the real numbers, the way Analysis computes it. An AND gate adds its children's costs and multiplies their
 * chances; an OR gate takes the cheapest child and combines the chances as 1 - (1 - p1)(1 - p2)... A test checks that the results equal Analysis.
 */
public record GateSteps(String cost, String chance, double minCost, double probability) {

    public static GateSteps of(Node.Gate g) {
        List<Analysis> children = g.children().stream().map(Analysis::of).toList();
        List<String> costs = new ArrayList<>();
        List<String> chances = new ArrayList<>();
        double cost = g.kind() == Node.Kind.AND ? 0 : Analysis.IMPOSSIBLE;
        double p = 1.0;
        for (Analysis a : children) {
            costs.add(AttackTreeDemo.number(a.minCost()));
            if (g.kind() == Node.Kind.AND) {
                cost += a.minCost();
                p *= a.probability();
                chances.add(AttackTreeDemo.decimal(a.probability()));
            } else {
                cost = Math.min(cost, a.minCost());
                p *= 1.0 - a.probability();
                chances.add("(1-" + AttackTreeDemo.decimal(a.probability()) + ")");
            }
        }
        if (g.kind() == Node.Kind.AND) {
            return new GateSteps(String.join(" + ", costs) + " = " + AttackTreeDemo.number(cost), String.join(" x ", chances) + String.format(" = %.0f%%", p * 100), cost, p);
        }
        return new GateSteps("min(" + String.join(", ", costs) + ") = " + AttackTreeDemo.number(cost), "1 - " + String.join("", chances) + String.format(" = %.0f%%", (1 - p) * 100), cost, 1 - p);
    }
}
