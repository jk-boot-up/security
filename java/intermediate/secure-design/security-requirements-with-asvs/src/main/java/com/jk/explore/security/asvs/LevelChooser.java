package com.jk.explore.security.asvs;

import java.util.ArrayList;
import java.util.List;

/**
 * A small decision model for the verification level. Every application starts at L1. Customer accounts or personal data raise it to L2.
 * Moving money, under regulation, with highly sensitive data, raises it to L3. The reasons are kept, because a level nobody can explain is
 * a level nobody will defend in a review.
 */
final class LevelChooser {

    record Choice(Level level, List<String> reasons) {
    }

    private LevelChooser() {
    }

    static Choice choose(Profile p) {
        List<String> reasons = new ArrayList<>();
        Level level = Level.L1;
        reasons.add("every application starts at L1, the floor");
        if (p.holdsAccounts() || p.data() != Profile.Sensitivity.LOW) {
            level = Level.L2;
            reasons.add("it holds customer accounts or personal data, so L2");
        }
        if (p.movesMoney() && p.regulated() && p.data() == Profile.Sensitivity.HIGH) {
            level = Level.L3;
            reasons.add("it moves money, is regulated and holds highly sensitive data, so L3");
        }
        return new Choice(level, List.copyOf(reasons));
    }
}
