package com.jk.explore.security.threats;

/** A risk score turned into a word: up to 5 is low, up to 12 is medium, above that is high. */
public enum Rating {
    LOW,
    MEDIUM,
    HIGH;

    public static Rating of(int score) {
        if (score <= 5) {
            return LOW;
        }
        return score <= 12 ? MEDIUM : HIGH;
    }
}
