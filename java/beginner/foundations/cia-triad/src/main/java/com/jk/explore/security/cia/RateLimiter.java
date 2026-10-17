package com.jk.explore.security.cia;

import java.util.HashMap;
import java.util.Map;

/** Availability control: each client may make only a fixed number of requests per tick. */
public class RateLimiter {

    private final int perClientPerTick;
    private final Map<String, Integer> seen = new HashMap<>();

    public RateLimiter(int perClientPerTick) {
        this.perClientPerTick = perClientPerTick;
    }

    /** True if this client may still be served in the current tick. */
    public boolean allow(String client) {
        int count = seen.merge(client, 1, Integer::sum);
        return count <= perClientPerTick;
    }
}
