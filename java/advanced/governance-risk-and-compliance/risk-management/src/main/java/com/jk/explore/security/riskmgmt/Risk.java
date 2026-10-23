package com.jk.explore.security.riskmgmt;

/** A line of the register: how often it happens in a year, how much one event costs, and the 1 to 5 scores a heat map would use. */
public record Risk(String id, String name, int likelihoodScore, int impactScore, Tri frequency, Tri loss) {

    public int score() {
        return likelihoodScore * impactScore;
    }

    /** The expected loss in a year if frequency and loss were independent: mean frequency times mean loss. */
    public double analyticAnnualLoss() {
        return frequency.mean() * loss.mean();
    }
}
