package com.jk.explore.security.depconfusion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A teaching class: how Gradle picks a version for a range like {@code [1.4,)}. It collects the versions that every repository lists in its
 * maven-metadata.xml, keeps those at or above the lower bound, and takes the highest. Versions compare number by number, so 99.0.0 beats 1.4.2.
 */
final class VersionSteps {
    private VersionSteps() {
    }

    /** Compares two versions number by number: 1.10 is higher than 1.9, and 1.4.2 is higher than 1.4. */
    static final Comparator<String> ORDER = (a, b) -> {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int c = Integer.compare(i < x.length ? Integer.parseInt(x[i]) : 0, i < y.length ? Integer.parseInt(y[i]) : 0);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    };

    /** The first number that differs, for example "99 > 1" for 99.0.0 against 1.4.2. */
    static String firstDifference(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (p != q) {
                return p + (p > q ? " > " : " < ") + q;
            }
        }
        return "equal";
    }

    /** The lower bound of an open range such as {@code [1.4,)}. */
    static String lowerBound(String range) {
        if (!range.startsWith("[") || !range.endsWith(",)")) {
            throw new IllegalArgumentException("not an open range: " + range);
        }
        return range.substring(1, range.length() - 2);
    }

    static boolean inRange(String version, String range) {
        return ORDER.compare(version, lowerBound(range)) >= 0;
    }

    static Optional<String> highest(List<String> versions, String range) {
        return versions.stream().filter(v -> inRange(v, range)).max(ORDER);
    }
}
