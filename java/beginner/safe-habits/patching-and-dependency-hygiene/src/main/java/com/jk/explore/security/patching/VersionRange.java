package com.jk.explore.security.patching;

import java.util.ArrayList;
import java.util.List;

/** A range such as ">=1.5.0 <1.8.3": space-separated comparators that must all hold. Comparators are >=, >, <=, < and =. */
final class VersionRange {

    private record Bound(String operator, SemVer version) {
        boolean holds(SemVer other) {
            int c = other.compareTo(version);
            return switch (operator) {
                case ">=" -> c >= 0;
                case ">" -> c > 0;
                case "<=" -> c <= 0;
                case "<" -> c < 0;
                default -> c == 0;
            };
        }
    }

    private final String text;
    private final List<Bound> bounds = new ArrayList<>();

    private VersionRange(String text) {
        this.text = text;
    }

    static VersionRange parse(String text) {
        VersionRange range = new VersionRange(text.trim());
        if (range.text.isEmpty()) {
            throw new IllegalArgumentException("empty version range");
        }
        for (String part : range.text.split("\\s+")) {
            String operator = part.startsWith(">=") || part.startsWith("<=") ? part.substring(0, 2)
                    : part.startsWith(">") || part.startsWith("<") || part.startsWith("=") ? part.substring(0, 1) : null;
            if (operator == null) {
                throw new IllegalArgumentException("a comparator must start with >=, >, <=, < or =: " + part);
            }
            range.bounds.add(new Bound(operator, SemVer.parse(part.substring(operator.length()))));
        }
        return range;
    }

    boolean contains(SemVer version) {
        return bounds.stream().allMatch(bound -> bound.holds(version));
    }

    @Override
    public String toString() {
        return text;
    }
}
