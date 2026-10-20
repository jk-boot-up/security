package com.jk.explore.security.attacktree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chooses defences by what they do to the tree: at each step, the one that most raises the cheapest attack. */
public final class Planner {

    /** One round of the plan: the defence chosen and what the tree says afterwards. */
    public record Step(Defence defence, Analysis after, int totalSpent) {
    }

    private Planner() {
    }

    /** What each defence alone would do to the baseline tree, best first. */
    public static List<Step> rank(Node tree, List<Defence> options) {
        List<Step> steps = new ArrayList<>();
        for (Defence d : options) {
            steps.add(new Step(d, Analysis.of(d.applyTo(tree)), d.price()));
        }
        steps.sort(byEffect());
        return steps;
    }

    /** Greedy plan: keep adding the defence that raises the cheapest path most, until the cheapest path costs at least {@code target}. */
    public static List<Step> plan(Node tree, List<Defence> options, double target) {
        List<Step> plan = new ArrayList<>();
        List<Defence> left = new ArrayList<>(options);
        Node current = tree;
        int spent = 0;
        while (Analysis.of(current).minCost() < target && !left.isEmpty()) {
            Node base = current;
            Step best = left.stream().map(d -> new Step(d, Analysis.of(d.applyTo(base)), d.price())).sorted(byEffect()).findFirst().orElseThrow();
            if (Analysis.of(base).minCost() >= best.after().minCost() && Analysis.of(base).probability() <= best.after().probability()) {
                break; // nothing left helps
            }
            spent += best.defence().price();
            current = best.defence().applyTo(current);
            left.remove(best.defence());
            plan.add(new Step(best.defence(), Analysis.of(current), spent));
        }
        return plan;
    }

    private static Comparator<Step> byEffect() {
        return Comparator.comparingDouble((Step s) -> s.after().minCost()).reversed()
                .thenComparingDouble(s -> s.after().probability())
                .thenComparingInt(s -> s.defence().price());
    }
}
