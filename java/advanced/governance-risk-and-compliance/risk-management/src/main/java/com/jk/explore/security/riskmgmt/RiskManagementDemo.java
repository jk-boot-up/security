package com.jk.explore.security.riskmgmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Prints the five acts. Everything is seeded and needs no date and no network, so every run prints the same text. */
public class RiskManagementDemo {

    static final int TRIALS = 50_000;
    static final long SEED = 20260101L;
    static final double BUDGET = 800_000;
    static final double APPETITE_LIMIT = 5_000_000;
    static final double APPETITE_CHANCE = 0.05;
    static final double BIN = 2_000_000;
    static final int BINS = 10;

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String money(double x) {
        if (Math.abs(x) >= 1_000_000) {
            return String.format("$%.1fM", x / 1_000_000);
        }
        return String.format("$%dk", Math.round(x / 1000));
    }

    static String pct(double x) {
        return String.format("%.1f%%", x * 100);
    }

    /** A number with at most three decimals and no trailing zeros: 0.05, 0.15, 2. */
    static String num(double x) {
        String s = String.format("%.3f", x);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    static String names(List<Control> cs) {
        return cs.isEmpty() ? "none" : String.join(", ", cs.stream().map(Control::id).toList());
    }

    static String range(Tri t, boolean dollars) {
        return dollars ? money(t.min()) + " " + money(t.mode()) + " " + money(t.max()) : num(t.min()) + " " + num(t.mode()) + " " + num(t.max());
    }

    static String effects(Control c) {
        List<String> parts = new ArrayList<>();
        for (Risk r : Register.RISKS) {
            Control.Effect e = c.effects().get(r.id());
            if (e == null) {
                continue;
            }
            String what = r.id();
            if (e.frequencyFactor() != 1) {
                parts.add(what + " events x" + num(e.frequencyFactor()));
                what = "";
            }
            if (e.lossFactor() != 1) {
                parts.add((what.isEmpty() ? "" : what + " ") + "loss x" + num(e.lossFactor()));
            }
        }
        return String.join(", ", parts);
    }

    /** The range read at a draw, with the numbers in: below the most likely point from the least, above it from the most. */
    static String inverse(Tri t, double u, boolean dollars) {
        java.util.function.DoubleFunction<String> v = x -> dollars ? money(x) : num(x);
        double split = (t.mode() - t.min()) / (t.max() - t.min());
        if (u < split) {
            return String.format("below: %s + sqrt(%.3f x %s x %s) = %s", v.apply(t.min()), u, v.apply(t.max() - t.min()), v.apply(t.mode() - t.min()), v.apply(t.at(u)));
        }
        return String.format("above: %s - sqrt(%.3f x %s x %s) = %s", v.apply(t.max()), 1 - u, v.apply(t.max() - t.min()), v.apply(t.max() - t.mode()), v.apply(t.at(u)));
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        List<Risk> risks = Register.RISKS;
        List<Control> controls = Register.CONTROLS;
        Simulation sim = new Simulation(risks, TRIALS, SEED);

        // ONE. One risk, by hand.
        Risk rw = risks.get(0);
        double[] rwYears = sim.annualLosses(0, List.of());
        out.append("ONE. One risk, from ranges to money: " + rw.id() + ".\n");
        out.append("  THE INPUT, AN EXPERT'S RANGES (least, most likely, most):\n");
        out.append(String.format("    events a year   %s%n", range(rw.frequency(), false)));
        out.append(String.format("    loss of one     %s%n", range(rw.loss(), true)));
        out.append(String.format("    heat map        likelihood %d x impact %d = score %d%n", rw.likelihoodScore(), rw.impactScore(), rw.score()));
        out.append("  THE AVERAGE OF A RANGE, (least + most likely + most) / 3:\n");
        out.append(String.format("    events a year   (%s) / 3 = %s%n", range(rw.frequency(), false).replace(" ", " + "), num(rw.frequency().mean())));
        out.append(String.format("    loss of one     (%s) / 3 = %s%n", range(rw.loss(), true).replace(" ", " + "), money(rw.loss().mean())));
        out.append(String.format("    expected loss   %s x %s = %s a year%n", num(rw.frequency().mean()), money(rw.loss().mean()), money(rw.analyticAnnualLoss())));
        int year = IntStream.range(0, TRIALS).filter(t -> YearSteps.of(sim, 0, t).events() == 1).findFirst().orElseThrow();
        YearSteps y = YearSteps.of(sim, 0, year);
        Tri f = rw.frequency();
        out.append("  ONE SIMULATED YEAR (year " + (year + 1) + "), THREE RANDOM DRAWS:\n");
        out.append(String.format("    how often: draw %.3f (most likely point at %.3f)%n", y.frequencyDraw(), y.split()));
        out.append("      " + inverse(rw.frequency(), y.frequencyDraw(), false) + " a year\n");
        out.append(String.format("    how many: draw %.3f%n", y.eventsDraw()));
        out.append(String.format("      P(none) = e^-%.3f = %.3f, P(at most 1) = %.3f%n", y.rate(), y.atMost().get(0), y.atMost().get(1)));
        out.append(String.format("      %.3f < %.3f <= %.3f, so 1 event%n", y.atMost().get(0), y.eventsDraw(), y.atMost().get(1)));
        Tri l = rw.loss();
        out.append(String.format("    how much: draw %.3f (most likely point at %.3f)%n", y.lossDraws().get(0), (l.mode() - l.min()) / (l.max() - l.min())));
        out.append("      " + inverse(l, y.lossDraws().get(0), true) + "\n");
        out.append("  THE YEAR'S LOSS: " + money(y.total()) + "\n");
        out.append("  THE RESULT OF " + TRIALS + " YEARS:\n");
        out.append(String.format("    years with no %-12s%s%n", rw.id(), pct(Arrays.stream(rwYears).filter(x -> x == 0).count() / (double) TRIALS)));
        out.append(String.format("    expected loss a year      %s%n", money(Simulation.mean(rwYears))));
        out.append(String.format("    1 year in 10 loses over   %s%n", money(Simulation.percentile(rwYears, 0.9))));

        // TWO. The register.
        out.append("TWO. The register: the heat map against money.\n");
        out.append("  THE INPUT, " + risks.size() + " RISKS (score = likelihood x impact):\n");
        out.append(String.format("    %-14s %-6s %-15s %s%n", "risk", "score", "events a year", "loss of one event"));
        for (Risk r : risks) {
            out.append(String.format("    %-14s %-6s %-15s %s%n", r.id(), r.likelihoodScore() + "x" + r.impactScore() + "=" + r.score(),
                    range(r.frequency(), false), range(r.loss(), true)));
        }
        double[] mean = new double[risks.size()];
        out.append("  THE RESULT, " + TRIALS + " SIMULATED YEARS:\n");
        out.append(String.format("    %-14s %5s  %-15s %s%n", "risk", "score", "expected a year", "1 year in 10 over"));
        for (int i = 0; i < risks.size(); i++) {
            double[] losses = sim.annualLosses(i, List.of());
            mean[i] = Simulation.mean(losses);
            out.append(String.format("    %-14s %5d  %-15s %s%n", risks.get(i).id(), risks.get(i).score(), money(mean[i]), money(Simulation.percentile(losses, 0.9))));
        }
        List<String> byScore = risks.stream().sorted(Comparator.comparingInt(Risk::score).reversed().thenComparing(Risk::id)).limit(3).map(Risk::id).toList();
        List<Integer> order = new ArrayList<>(IntStream.range(0, risks.size()).boxed().toList());
        order.sort(Comparator.comparingDouble((Integer i) -> mean[i]).reversed());
        List<String> byLoss = order.stream().limit(3).map(i -> risks.get(i).id()).toList();
        out.append("  TWO RANKINGS, THE TOP THREE:\n");
        out.append("    by heat map score  " + String.join(", ", byScore) + "\n");
        out.append("    by expected loss   " + String.join(", ", byLoss) + "\n");
        out.append("    in both lists      " + byScore.stream().filter(byLoss::contains).count() + " of 3\n");

        // THREE. The whole register.
        double[] all = sim.portfolio(List.of());
        double allMean = Simulation.mean(all);
        out.append("THREE. The whole register: the bad years.\n");
        out.append("  THE INPUT: the " + risks.size() + " risks added up in each of " + TRIALS + " years\n");
        out.append("  HOW THE YEARS SPREAD (the register's loss in a year):\n");
        int[] counts = new int[BINS + 1];
        for (double x : all) {
            counts[Math.min(BINS, (int) (x / BIN))]++;
        }
        for (int b = 0; b <= BINS; b++) {
            String label = b < BINS ? String.format("$%dM to $%dM", Math.round(b * BIN / 1e6), Math.round((b + 1) * BIN / 1e6)) : String.format("$%dM or more", Math.round(BINS * BIN / 1e6));
            double share = counts[b] / (double) TRIALS;
            out.append(String.format("    %-13s %5s  %s%n", label, pct(share), "#".repeat((int) Math.round(share * 100))));
        }
        out.append("  THE RESULT:\n");
        out.append("    expected loss a year         " + money(allMean) + "\n");
        out.append("    check: the 8 averages added  " + money(risks.stream().mapToDouble(Risk::analyticAnnualLoss).sum()) + "\n");
        out.append("    1 year in 10 loses more than " + money(Simulation.percentile(all, 0.9)) + "\n");
        out.append("    1 year in 20 loses more than " + money(Simulation.percentile(all, 0.95)) + "\n");
        out.append("    years losing more than " + money(APPETITE_LIMIT) + " " + pct(Simulation.exceed(all, APPETITE_LIMIT)) + "\n");
        double small = Simulation.mean(new Simulation(risks, 1_000, SEED).portfolio(List.of()));
        out.append("  HOW MANY YEARS ARE ENOUGH:\n");
        out.append(String.format("    %-6d years  %s, %s away%n", 1_000, money(small), pct(Math.abs(small - allMean) / allMean)));
        out.append(String.format("    %-6d years  %s%n", TRIALS, money(allMean)));
        out.append("  WHICH INPUT MATTERS (the most likely loss 50% higher):\n");
        out.append(String.format("    %-12s %-27s %s%n", "risk", "events x change / 3", "simulated"));
        for (int i : order.subList(0, 3)) {
            Risk r = risks.get(i);
            Risk bigger = new Risk(r.id(), r.name(), r.likelihoodScore(), r.impactScore(), r.frequency(), new Tri(r.loss().min(), r.loss().mode() * 1.5, r.loss().max()));
            double changed = Simulation.mean(sim.with(i, bigger).portfolio(List.of()));
            double byHand = r.frequency().mean() * (r.loss().mode() * 0.5) / 3;
            out.append(String.format("    %-12s %-27s +%s%n", r.id(), num(r.frequency().mean()) + " x " + money(r.loss().mode() * 0.5) + " / 3 = " + money(byHand), money(changed - allMean)));
        }

        // FOUR. Treatment.
        out.append("FOUR. Controls within a budget of " + money(BUDGET) + ".\n");
        out.append("  THE INPUT, " + controls.size() + " CONTROLS (cost a year, what each changes):\n");
        for (Control c : controls) {
            out.append(String.format("    %-9s %5s  %s%n", c.id(), money(c.annualCost()), effects(c)));
        }
        out.append("  EACH CONTROL ALONE (loss avoided - cost = net benefit):\n");
        for (Control c : controls) {
            double avoided = allMean - Simulation.mean(sim.portfolio(List.of(c)));
            out.append(String.format("    %-9s %6s - %5s = %s%n", c.id(), money(avoided), money(c.annualCost()), money(avoided - c.annualCost())));
        }
        int sets = (1 << controls.size()) - 1;
        out.append("  HOW THE BEST SET IS FOUND:\n");
        out.append("    sets of " + controls.size() + " controls   " + sets + " (2^" + controls.size() + " - 1)\n");
        out.append("    within " + money(BUDGET) + "         " + Optimizer.setsWithin(controls, BUDGET) + ", each on the same " + TRIALS + " years\n");
        Optimizer.Choice best = Optimizer.best(sim, controls, BUDGET);
        Optimizer.Choice cheap = Optimizer.cheapestFirst(sim, controls, BUDGET);
        out.append("  THE BEST SET:\n");
        appendChoice(out, best, allMean);
        out.append("  CHEAPEST FIRST, THE RULE OF THUMB:\n");
        appendChoice(out, cheap, allMean);
        out.append("  THE DIFFERENCE:\n");
        out.append("    the best set gains " + money(best.netBenefit() - cheap.netBenefit()) + " a year more\n");

        // FIVE. Appetite.
        double[] after = sim.portfolio(best.controls());
        double[] everything = sim.portfolio(controls);
        double allCost = controls.stream().mapToDouble(Control::annualCost).sum();
        out.append("FIVE. The appetite, and what is left.\n");
        out.append("  THE APPETITE, THE BOARD'S NUMBER:\n");
        out.append("    at most " + pct(APPETITE_CHANCE) + " of years may lose more than " + money(APPETITE_LIMIT) + "\n");
        out.append("  BEFORE ANY CONTROL:\n");
        appendAppetite(out, all);
        out.append("  AFTER THE BEST SET (" + money(best.cost()) + "):\n");
        appendAppetite(out, after);
        out.append("    residual risk     expected " + money(Simulation.mean(after)) + ", 1 year in 20 over " + money(Simulation.percentile(after, 0.95)) + "\n");
        out.append("  WITH ALL " + controls.size() + " CONTROLS (" + money(allCost) + "):\n");
        appendAppetite(out, everything);
        Optimizer.Choice meets = Optimizer.cheapestWithinAppetite(sim, controls, APPETITE_LIMIT, APPETITE_CHANCE).orElseThrow();
        out.append("  THE CHEAPEST SET WITHIN THE APPETITE:\n");
        out.append("    " + (meets.controls().size() == controls.size() ? "all " + controls.size() + " controls: no smaller set is enough" : names(meets.controls())) + "\n");
        out.append("    cost " + money(meets.cost()) + ", " + money(meets.cost() - BUDGET) + " over the budget\n");
        out.append("  THE DECISION IS THE BOARD'S:\n");
        out.append("    spend more, insure the rest, or change the appetite\n");
        out.append("  WHAT THE MODEL CANNOT KNOW:\n");
        out.append("    the ranges are an expert's guesses, not measurements\n");
        out.append("    the risks are independent here; real incidents cluster\n");
        out.append("    an incident nobody imagined is not in the register\n");
        return out.toString();
    }

    private static void appendChoice(StringBuilder out, Optimizer.Choice c, double baseline) {
        out.append("    controls        " + names(c.controls()) + "\n");
        out.append("    cost            " + money(c.cost()) + "\n");
        out.append("    expected loss   " + money(baseline) + " -> " + money(c.meanLoss()) + "\n");
        out.append("    net benefit     " + money(baseline - c.meanLoss()) + " - " + money(c.cost()) + " = " + money(c.netBenefit()) + "\n");
    }

    private static void appendAppetite(StringBuilder out, double[] years) {
        double over = Simulation.exceed(years, APPETITE_LIMIT);
        out.append("    years over " + money(APPETITE_LIMIT) + "  " + pct(over) + "  " + (over <= APPETITE_CHANCE ? "WITHIN" : "OUTSIDE") + "\n");
    }
}
