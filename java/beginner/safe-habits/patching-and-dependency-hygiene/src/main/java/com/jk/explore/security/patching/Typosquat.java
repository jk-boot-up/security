package com.jk.explore.security.patching;

import java.util.Collection;

/** Spots a package name that is one edit away from a known one, the trick of a copy published under a nearly identical name. */
final class Typosquat {

    private Typosquat() {
    }

    static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            previous = current;
        }
        return previous[b.length()];
    }

    /** True if the name is not itself known but is one edit from a name that is. */
    static boolean looksLikeACopy(String candidate, Collection<String> known) {
        if (known.contains(candidate)) {
            return false;
        }
        return known.stream().anyMatch(name -> distance(candidate, name) == 1);
    }
}
