package com.jk.explore.security.leastprivilegespring;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Access that expires by itself: a permission given to one person for a window of time, judged by an injected clock. */
final class Grants {

    record Grant(String user, String permission, long fromSecond, long toSecond) {
    }

    private final List<Grant> grants = new CopyOnWriteArrayList<>();
    private final Clock clock;

    Grants(Clock clock) {
        this.clock = clock;
    }

    void give(String user, String permission, long fromSecond, long toSecond) {
        grants.add(new Grant(user, permission, fromSecond, toSecond));
    }

    /** Whether the person holds the permission right now: from the first second to the last second, both included. */
    boolean active(String user, String permission) {
        long now = clock.instant().getEpochSecond();
        return grants.stream().anyMatch(g -> g.user().equals(user) && g.permission().equals(permission)
                && now >= g.fromSecond() && now <= g.toSecond());
    }
}
