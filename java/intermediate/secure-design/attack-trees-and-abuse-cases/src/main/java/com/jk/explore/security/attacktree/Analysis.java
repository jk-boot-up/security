package com.jk.explore.security.attacktree;

import java.util.ArrayList;
import java.util.List;

/**
 * What a tree says about an attacker: the cheapest way to reach the goal (its cost and its steps), and the chance that some way succeeds. Costs add along AND gates and
 * the cheapest child wins at OR gates; probabilities multiply along AND gates and combine as 1 - (1-p1)(1-p2)... at OR gates, assuming the steps are independent.
 */
public record Analysis(double minCost, List<String> cheapestPath, double probability) {

    public static final double IMPOSSIBLE = Double.POSITIVE_INFINITY;

    public boolean possible() {
        return minCost != IMPOSSIBLE;
    }

    public static Analysis of(Node n) {
        return switch (n) {
            case Node.Leaf l -> l.probability() <= 0 ? new Analysis(IMPOSSIBLE, List.of(), 0) : new Analysis(l.cost(), List.of(l.id()), l.probability());
            case Node.Gate g -> g.kind() == Node.Kind.OR ? or(g) : and(g);
        };
    }

    private static Analysis or(Node.Gate g) {
        Analysis best = new Analysis(IMPOSSIBLE, List.of(), 0);
        double none = 1.0;
        for (Node c : g.children()) {
            Analysis a = of(c);
            none *= 1.0 - a.probability();
            if (a.minCost() < best.minCost()) {
                best = a;
            }
        }
        return new Analysis(best.minCost(), best.cheapestPath(), 1.0 - none);
    }

    private static Analysis and(Node.Gate g) {
        double cost = 0;
        double p = 1.0;
        List<String> path = new ArrayList<>();
        for (Node c : g.children()) {
            Analysis a = of(c);
            cost += a.minCost();
            p *= a.probability();
            path.addAll(a.cheapestPath());
        }
        return cost == IMPOSSIBLE || Double.isInfinite(cost) ? new Analysis(IMPOSSIBLE, List.of(), 0) : new Analysis(cost, path, p);
    }
}
