package com.jk.explore.security.riskmgmt;

import java.util.Comparator;
import java.util.List;

public class RiskManagementDemo {

    static final int TRIALS = 50_000;
    static final long SEED = 20260101L;
    static final double BUDGET = 800_000;
    static final double APPETITE_LIMIT = 5_000_000;
    static final double APPETITE_CHANCE = 0.05;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String money(double x) {
        return x >= 1_000_000 ? String.format("$%.1fM", x / 1_000_000) : String.format("$%dk", Math.round(x / 1000));
    }

    static String pct(double x) {
        return String.format("%.1f%%", x * 100);
    }

    static String names(List<Control> cs) {
        return cs.isEmpty() ? "none" : String.join(", ", cs.stream().map(Control::id).toList());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        List<Risk> risks = Register.RISKS;
        Simulation sim = new Simulation(risks, TRIALS, SEED);

        out.append("--- the register\n");
        out.append("risks: " + risks.size() + ", controls: " + Register.CONTROLS.size() + ", simulated years: " + TRIALS + "\n");
        double[] mean = new double[risks.size()];
        for (int i = 0; i < risks.size(); i++) {
            double[] losses = sim.annualLosses(i, List.of());
            mean[i] = Simulation.mean(losses);
            out.append(String.format("%-14s score %2d, expected loss a year %6s, 1 year in 10 loses more than %s%n", risks.get(i).id(), risks.get(i).score(), money(mean[i]), money(Simulation.percentile(losses, 0.9))));
        }
        List<String> byScore = risks.stream().sorted(Comparator.comparingInt(Risk::score).reversed().thenComparing(Risk::id)).limit(3).map(Risk::id).toList();
        List<Integer> order = new java.util.ArrayList<>(java.util.stream.IntStream.range(0, risks.size()).boxed().toList());
        order.sort(Comparator.comparingDouble((Integer i) -> mean[i]).reversed());
        List<String> byLoss = order.stream().limit(3).map(i -> risks.get(i).id()).toList();
        out.append("top three by heat map score: " + String.join(", ", byScore) + "\n");
        out.append("top three by expected loss: " + String.join(", ", byLoss) + "\n");
        out.append("in both lists: " + byScore.stream().filter(byLoss::contains).count() + " of 3\n");

        out.append("--- the whole register\n");
        double[] all = sim.portfolio(List.of());
        double analytic = risks.stream().mapToDouble(Risk::analyticAnnualLoss).sum();
        out.append("expected loss a year: " + money(Simulation.mean(all)) + ", by mean frequency times mean loss: " + money(analytic) + "\n");
        out.append("1 year in 10 loses more than " + money(Simulation.percentile(all, 0.9)) + ", 1 year in 20 more than " + money(Simulation.percentile(all, 0.95)) + "\n");
        out.append("chance of losing more than " + money(APPETITE_LIMIT) + " in a year: " + pct(Simulation.exceed(all, APPETITE_LIMIT)) + "\n");
        double small = Simulation.mean(new Simulation(risks, 1_000, SEED).portfolio(List.of()));
        out.append("the same estimate with 1000 years: " + money(small) + ", " + pct(Math.abs(small - Simulation.mean(all)) / Simulation.mean(all)) + " away\n");

        out.append("--- which input matters\n");
        for (int i : order.subList(0, 3)) {
            Risk r = risks.get(i);
            Risk bigger = new Risk(r.id(), r.name(), r.likelihoodScore(), r.impactScore(), r.frequency(), new Tri(r.loss().min(), r.loss().mode() * 1.5, r.loss().max()));
            double changed = Simulation.mean(sim.with(i, bigger).portfolio(List.of()));
            out.append(r.id() + ": a most likely loss 50% higher moves the expected loss by " + money(changed - Simulation.mean(all)) + "\n");
        }

        out.append("--- treatment within a budget of " + money(BUDGET) + "\n");
        Optimizer.Choice best = Optimizer.best(sim, Register.CONTROLS, BUDGET);
        Optimizer.Choice cheap = Optimizer.cheapestFirst(sim, Register.CONTROLS, BUDGET);
        out.append("best set: " + names(best.controls()) + ", cost " + money(best.cost()) + ", expected loss " + money(best.meanLoss()) + ", net benefit " + money(best.netBenefit()) + "\n");
        out.append("cheapest first: " + names(cheap.controls()) + ", cost " + money(cheap.cost()) + ", expected loss " + money(cheap.meanLoss()) + ", net benefit " + money(cheap.netBenefit()) + "\n");
        out.append("the best set beats cheapest first by " + money(best.netBenefit() - cheap.netBenefit()) + " a year\n");

        out.append("--- appetite and what is left\n");
        double[] after = sim.portfolio(best.controls());
        double before = Simulation.exceed(all, APPETITE_LIMIT);
        double left = Simulation.exceed(after, APPETITE_LIMIT);
        out.append("appetite: at most " + pct(APPETITE_CHANCE) + " chance of losing more than " + money(APPETITE_LIMIT) + " in a year\n");
        out.append("before: " + pct(before) + ", within appetite: " + (before <= APPETITE_CHANCE ? "yes" : "no") + "\n");
        out.append("after the best set: " + pct(left) + ", within appetite: " + (left <= APPETITE_CHANCE ? "yes" : "no") + "\n");
        out.append("residual: expected loss " + money(Simulation.mean(after)) + ", 1 year in 20 loses more than " + money(Simulation.percentile(after, 0.95)) + "\n");

        double everything = Simulation.exceed(sim.portfolio(Register.CONTROLS), APPETITE_LIMIT);
        double allCost = Register.CONTROLS.stream().mapToDouble(Control::annualCost).sum();
        out.append("with all " + Register.CONTROLS.size() + " controls (cost " + money(allCost) + "): " + pct(everything) + ", within appetite: " + (everything <= APPETITE_CHANCE ? "yes" : "no") + "\n");
        out.append("reaching the appetite costs more than the budget: spend more, transfer the rest with insurance, or change the appetite\n");

        out.append("--- the bill\n");
        out.append("every input is an expert's range, not a measurement: the output is only as good as the workshop\n");
        out.append("the risks are taken as independent here, and real incidents arrive together\n");
        out.append("a number is easy to compare and easy to trust too much: show the range and the tail with it\n");
        return out.toString();
    }
}
