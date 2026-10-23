package com.jk.explore.security.riskmgmt;

import java.util.ArrayList;
import java.util.List;

/** Chooses the set of controls that gives the largest net benefit (the loss avoided minus what the controls cost) within a budget, by trying every set. */
public final class Optimizer {

    private Optimizer() {
    }

    /** A set of controls with what it costs and what it saves. */
    public record Choice(List<Control> controls, double cost, double meanLoss, double netBenefit) {
    }

    public static Choice best(Simulation sim, List<Control> available, double budget) {
        double baseline = Simulation.mean(sim.portfolio(List.of()));
        Choice best = new Choice(List.of(), 0, baseline, 0);
        for (int mask = 1; mask < (1 << available.size()); mask++) {
            List<Control> set = new ArrayList<>();
            double cost = 0;
            for (int i = 0; i < available.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    set.add(available.get(i));
                    cost += available.get(i).annualCost();
                }
            }
            if (cost > budget) {
                continue;
            }
            double mean = Simulation.mean(sim.portfolio(set));
            double net = baseline - mean - cost;
            if (net > best.netBenefit()) {
                best = new Choice(set, cost, mean, net);
            }
        }
        return best;
    }

    /** The same budget spent on the cheapest controls first, as a rule of thumb. */
    public static Choice cheapestFirst(Simulation sim, List<Control> available, double budget) {
        double baseline = Simulation.mean(sim.portfolio(List.of()));
        List<Control> sorted = available.stream().sorted(java.util.Comparator.comparingDouble(Control::annualCost)).toList();
        List<Control> set = new ArrayList<>();
        double cost = 0;
        for (Control c : sorted) {
            if (cost + c.annualCost() <= budget) {
                set.add(c);
                cost += c.annualCost();
            }
        }
        double mean = Simulation.mean(sim.portfolio(set));
        return new Choice(set, cost, mean, baseline - mean - cost);
    }
}
