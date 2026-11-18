package com.jk.explore.security.linuxhard;

import java.util.List;

/** The scan as a build step: the build fails when any finding is at least this severe. */
public record BuildGate(Severity failAt) {

    public boolean passes(List<Finding> findings) {
        return findings.stream().noneMatch(f -> f.severity().compareTo(failAt) >= 0);
    }
}
