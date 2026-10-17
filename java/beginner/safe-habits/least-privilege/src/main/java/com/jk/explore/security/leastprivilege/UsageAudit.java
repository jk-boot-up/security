package com.jk.explore.security.leastprivilege;

import java.util.List;

/** Finds permissions that were granted and have not been used lately: privilege creep. */
final class UsageAudit {

    /** A permission held by a user, and the day it was last used (-1 if it never was). */
    record Holding(String user, String permission, long lastUsedDay) {
    }

    private UsageAudit() {
    }

    static List<Holding> unused(List<Holding> holdings, long today, long days) {
        return holdings.stream().filter(h -> h.lastUsedDay() < 0 || today - h.lastUsedDay() > days).toList();
    }
}
