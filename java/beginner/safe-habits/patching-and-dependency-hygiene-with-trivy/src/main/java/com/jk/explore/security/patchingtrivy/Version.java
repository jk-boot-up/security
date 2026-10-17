package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.List;

/**
 * A Maven-style version: numbers separated by dots, optionally followed by a qualifier after a dash. Numbers are compared as numbers
 * (1.10 is newer than 1.9), missing numbers count as zero (1.9 equals 1.9.0), and a qualifier makes a version older (2.0-beta9 is older than 2.0).
 */
record Version(List<Integer> numbers, String qualifier) implements Comparable<Version> {

    static Version parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("empty version");
        }
        int dash = trimmed.indexOf('-');
        String numberPart = dash < 0 ? trimmed : trimmed.substring(0, dash);
        String qualifier = dash < 0 ? "" : trimmed.substring(dash + 1);
        List<Integer> numbers = new ArrayList<>();
        for (String part : numberPart.split("\\.", -1)) {
            if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("not a version: " + text);
            }
            numbers.add(Integer.parseInt(part));
        }
        return new Version(List.copyOf(numbers), qualifier);
    }

    @Override
    public int compareTo(Version other) {
        int length = Math.max(numbers.size(), other.numbers.size());
        for (int i = 0; i < length; i++) {
            int mine = i < numbers.size() ? numbers.get(i) : 0;
            int theirs = i < other.numbers.size() ? other.numbers.get(i) : 0;
            if (mine != theirs) {
                return Integer.compare(mine, theirs);
            }
        }
        if (qualifier.equals(other.qualifier)) {
            return 0;
        }
        if (qualifier.isEmpty()) {
            return 1;
        }
        return other.qualifier.isEmpty() ? -1 : qualifier.compareTo(other.qualifier);
    }

    boolean isOlderThan(Version other) {
        return compareTo(other) < 0;
    }

    @Override
    public String toString() {
        return String.join(".", numbers.stream().map(String::valueOf).toList()) + (qualifier.isEmpty() ? "" : "-" + qualifier);
    }
}
