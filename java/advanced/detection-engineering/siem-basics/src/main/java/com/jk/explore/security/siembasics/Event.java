package com.jk.explore.security.siembasics;

import java.time.Instant;

/** The one shape that every source is turned into: when (in UTC), who, from where, what, and whether it worked. */
public record Event(Instant at, String source, String host, String user, String ip, String action, String outcome, long rows) {

    public boolean failedLogin() {
        return action.equals("login") && outcome.equals("failure");
    }
}
