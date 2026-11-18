package com.jk.explore.security.secretscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A teaching class: the entropy that {@link Detector#entropy} computes in one loop, worked out in steps. Characters that appear
 * the same number of times give the same share, so they are added up as one group: k characters seen c times in n add
 * k x c/n x log2(n/c) bits.
 */
final class EntropySteps {

    /** One group: how many different characters appear {@code times} times, and the bits they add. */
    record Group(int characters, int times, int length, double bits) {

        String line() {
            String label = characters + (characters == 1 ? " character " : " characters ") + (times == 1 ? "once" : times == 2 ? "twice" : times + " times");
            return String.format("%-24s%d x %d/%d x log2(%d/%d) = %.3f", label + ":", characters, times, length, length, times, bits);
        }
    }

    private EntropySteps() {
    }

    static List<Group> groups(String s) {
        Map<Character, Integer> counts = new TreeMap<>();
        for (char c : s.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        Map<Integer, Integer> byTimes = new TreeMap<>();
        counts.values().forEach(t -> byTimes.merge(t, 1, Integer::sum));
        List<Group> groups = new ArrayList<>();
        int n = s.length();
        byTimes.forEach((times, k) -> {
            double p = (double) times / n;
            groups.add(new Group(k, times, n, k * p * Math.log(1 / p) / Math.log(2)));
        });
        return groups;
    }

    static double total(String s) {
        return groups(s).stream().mapToDouble(Group::bits).sum();
    }

    static long distinct(String s) {
        return s.chars().distinct().count();
    }
}
