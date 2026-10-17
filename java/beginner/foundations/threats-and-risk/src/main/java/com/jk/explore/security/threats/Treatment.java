package com.jk.explore.security.threats;

/** A proposed answer to one risk, what it costs, and the likelihood and impact left afterwards. */
public record Treatment(String riskId, Strategy strategy, String action, int cost, int newLikelihood, int newImpact) {
}
