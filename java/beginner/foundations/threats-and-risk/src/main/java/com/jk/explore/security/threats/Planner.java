package com.jk.explore.security.threats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chooses which treatments to pay for. Each treatment is worth the score it removes, divided by its
 * cost; the best value is bought first, while the budget lasts. Risks left without a funded treatment
 * are accepted as they are.
 */
public class Planner {

    public Plan plan(RiskRegister register, List<Treatment> options, int budget) {
        List<Treatment> byValue = options.stream()
                .filter(t -> reduction(register, t) > 0)
                .sorted(Comparator.comparingDouble((Treatment t) -> value(register, t)).reversed()
                        .thenComparing(Comparator.comparingInt((Treatment t) -> reduction(register, t)).reversed())
                        .thenComparing(Treatment::riskId))
                .toList();
        List<Treatment> funded = new ArrayList<>();
        List<Treatment> unfunded = new ArrayList<>();
        Map<String, int[]> rescored = new HashMap<>();
        int spent = 0;
        for (Treatment t : byValue) {
            if (spent + t.cost() <= budget) {
                spent += t.cost();
                funded.add(t);
                rescored.put(t.riskId(), new int[] {t.newLikelihood(), t.newImpact()});
            } else {
                unfunded.add(t);
            }
        }
        return new Plan(funded, unfunded, spent, budget, register.rescored(rescored));
    }

    /** The score a treatment removes from its risk. */
    static int reduction(RiskRegister register, Treatment t) {
        return register.byId(t.riskId()).score() - t.newLikelihood() * t.newImpact();
    }

    /** Score removed per unit of cost; a free treatment is worth its whole reduction. */
    static double value(RiskRegister register, Treatment t) {
        return (double) reduction(register, t) / Math.max(1, t.cost());
    }
}
