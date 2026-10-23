package com.jk.explore.security.riskmgmt;

import java.util.Map;

/** A treatment: what it costs a year, and for each risk it touches the factors by which it multiplies the frequency and the loss of an event. */
public record Control(String id, String name, double annualCost, Map<String, Effect> effects) {

    /** Factors of 1.0 change nothing; 0.3 keeps 30 per cent. */
    public record Effect(double frequencyFactor, double lossFactor) {
    }

    public Effect on(String riskId) {
        return effects.getOrDefault(riskId, new Effect(1, 1));
    }
}
