package com.jk.explore.security.threats;

/** A risk score turned into a word: up to 5 is low, up to 12 is medium, above that is high. */
public enum Rating {
    LOW(0, 5),
    MEDIUM(6, 12),
    HIGH(13, 25);

    private final int from;
    private final int to;

    Rating(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public static Rating of(int score) {
        if (score <= 5) {
            return LOW;
        }
        return score <= 12 ? MEDIUM : HIGH;
    }

    /** The scores this rating covers, such as "13 to 25". */
    public String range() {
        return from + " to " + to;
    }
}
