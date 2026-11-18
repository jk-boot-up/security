package com.jk.explore.security.riskmgmt;

import java.util.ArrayList;
import java.util.List;

/**
 * A teaching class: one simulated year of one risk, redone by hand from the same draws the simulation keeps. A test proves that the total is exactly the
 * simulation's loss for that year.
 */
public record YearSteps(double frequencyDraw, double split, double rate, double eventsDraw, List<Double> atMost, int events, List<Double> lossDraws,
        List<Double> losses, double total) {

    public static YearSteps of(Simulation sim, int riskIndex, int year) {
        Risk risk = sim.risk(riskIndex);
        Tri f = risk.frequency();
        // 1. how often this year: the frequency range read at the first draw
        double u1 = sim.frequencyDraw(riskIndex, year);
        double split = (f.mode() - f.min()) / (f.max() - f.min());
        double rate = f.at(u1);
        // 2. how many events: walk up P(0), P(at most 1), ... until the second draw is reached
        double u2 = sim.eventsDraw(riskIndex, year);
        List<Double> atMost = new ArrayList<>();
        double p = StrictMath.exp(-rate);
        double cumulative = p;
        atMost.add(cumulative);
        int events = 0;
        while (u2 > cumulative && events < Simulation.MAX_EVENTS) {
            events++;
            p *= rate / events;
            cumulative += p;
            atMost.add(cumulative);
        }
        // 3. what each event costs: the loss range read at one draw per event
        List<Double> draws = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        double total = 0;
        for (int j = 0; j < events; j++) {
            double u = sim.lossDraw(riskIndex, year, j);
            draws.add(u);
            losses.add(risk.loss().at(u));
            total += risk.loss().at(u);
        }
        return new YearSteps(u1, split, rate, u2, List.copyOf(atMost), events, List.copyOf(draws), List.copyOf(losses), total);
    }
}
