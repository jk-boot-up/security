package com.jk.explore.security.iacscan;

import java.util.List;

/** The findings a team has accepted so far: the build fails on a finding that is not in it, so the number can only go down. */
public final class Baseline {

    private Baseline() {
    }

    public static List<Finding> newFindings(List<Finding> now, List<Finding> accepted) {
        return now.stream().filter(f -> !accepted.contains(f)).toList();
    }

    public static List<Finding> fixed(List<Finding> now, List<Finding> accepted) {
        return accepted.stream().filter(f -> !now.contains(f)).toList();
    }
}
