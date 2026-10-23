package com.jk.explore.security.riskmgmt;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Monte Carlo over years. The random draws for each risk are made once and kept (common random numbers), so that two treatments are compared on the same
 * simulated years and a difference between them is the treatment, not the luck.
 */
public final class Simulation {

    static final int MAX_EVENTS = 25;

    private final List<Risk> risks;
    private final int trials;
    private final double[][] frequencyU;
    private final double[][] eventsU;
    private final double[][][] lossU;

    public Simulation(List<Risk> risks, int trials, long seed) {
        this.risks = risks;
        this.trials = trials;
        this.frequencyU = new double[risks.size()][trials];
        this.eventsU = new double[risks.size()][trials];
        this.lossU = new double[risks.size()][trials][MAX_EVENTS];
        for (int r = 0; r < risks.size(); r++) {
            RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed + 7919L * r);
            for (int t = 0; t < trials; t++) {
                frequencyU[r][t] = rng.nextDouble();
                eventsU[r][t] = rng.nextDouble();
                for (int j = 0; j < MAX_EVENTS; j++) {
                    lossU[r][t][j] = rng.nextDouble();
                }
            }
        }
    }

    private Simulation(List<Risk> risks, Simulation same) {
        this.risks = risks;
        this.trials = same.trials;
        this.frequencyU = same.frequencyU;
        this.eventsU = same.eventsU;
        this.lossU = same.lossU;
    }

    /** The same simulated years with one risk of the register replaced by another estimate. */
    public Simulation with(int riskIndex, Risk replacement) {
        java.util.ArrayList<Risk> changed = new java.util.ArrayList<>(risks);
        changed.set(riskIndex, replacement);
        return new Simulation(List.copyOf(changed), this);
    }

    public int trials() {
        return trials;
    }

    /** The number of events of a Poisson year with the given mean, found from one uniform draw by walking up the distribution function. */
    static int poisson(double lambda, double u) {
        double p = Math.exp(-lambda);
        double cumulative = p;
        int k = 0;
        while (u > cumulative && k < MAX_EVENTS) {
            k++;
            p *= lambda / k;
            cumulative += p;
        }
        return k;
    }

    /** The loss of each simulated year for one risk, with the factors of the chosen controls applied. */
    public double[] annualLosses(int riskIndex, List<Control> controls) {
        Risk risk = risks.get(riskIndex);
        double fFactor = 1;
        double lFactor = 1;
        for (Control c : controls) {
            fFactor *= c.on(risk.id()).frequencyFactor();
            lFactor *= c.on(risk.id()).lossFactor();
        }
        double[] losses = new double[trials];
        for (int t = 0; t < trials; t++) {
            double lambda = risk.frequency().at(frequencyU[riskIndex][t]) * fFactor;
            int events = poisson(lambda, eventsU[riskIndex][t]);
            double sum = 0;
            for (int j = 0; j < events; j++) {
                sum += risk.loss().at(lossU[riskIndex][t][j]) * lFactor;
            }
            losses[t] = sum;
        }
        return losses;
    }

    /** The loss of each simulated year for the whole register: the risks are taken as independent. */
    public double[] portfolio(List<Control> controls) {
        double[] total = new double[trials];
        for (int r = 0; r < risks.size(); r++) {
            double[] one = annualLosses(r, controls);
            for (int t = 0; t < trials; t++) {
                total[t] += one[t];
            }
        }
        return total;
    }

    public static double mean(double[] losses) {
        return Arrays.stream(losses).average().orElse(0);
    }

    /** The loss that a year stays under with probability p. */
    public static double percentile(double[] losses, double p) {
        double[] sorted = losses.clone();
        Arrays.sort(sorted);
        return sorted[(int) Math.min(sorted.length - 1, Math.floor(p * sorted.length))];
    }

    /** The share of years whose loss is more than the limit. */
    public static double exceed(double[] losses, double limit) {
        return Arrays.stream(losses).filter(x -> x > limit).count() / (double) losses.length;
    }
}
