package com.jk.explore.security.riskmgmt;

/** A three-point estimate (the least, the most likely and the most that an expert will say) as a triangular distribution. */
public record Tri(double min, double mode, double max) {

    public Tri {
        if (!(min <= mode && mode <= max)) {
            throw new IllegalArgumentException("need min <= mode <= max");
        }
    }

    /** The value at a given quantile u in [0, 1): the inverse of the distribution function. */
    public double at(double u) {
        double split = (max - min) == 0 ? 0 : (mode - min) / (max - min);
        if (u < split) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        }
        return max - Math.sqrt((1 - u) * (max - min) * (max - mode));
    }

    public double mean() {
        return (min + mode + max) / 3;
    }
}
