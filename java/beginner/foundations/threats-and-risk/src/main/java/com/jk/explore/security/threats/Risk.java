package com.jk.explore.security.threats;

/**
 * One thing that could go wrong. A threat is who or what could do harm, a vulnerability is the
 * weakness it would use, and the asset is what is harmed. Likelihood and impact are each scored
 * from 1 (lowest) to 5 (highest), and the risk score is their product.
 */
public record Risk(String id, String threat, String vulnerability, String asset, int likelihood, int impact) {

    public Risk {
        if (likelihood < 0 || likelihood > 5 || impact < 0 || impact > 5) {
            throw new IllegalArgumentException("likelihood and impact are scored from 0 to 5");
        }
    }

    public int score() {
        return likelihood * impact;
    }

    public Rating rating() {
        return Rating.of(score());
    }

    public Risk rescored(int newLikelihood, int newImpact) {
        return new Risk(id, threat, vulnerability, asset, newLikelihood, newImpact);
    }
}
