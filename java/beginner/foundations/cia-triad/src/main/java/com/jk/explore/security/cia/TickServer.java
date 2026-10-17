package com.jk.explore.security.cia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A order server that can do a fixed amount of work in one tick (a slice of simulated time, so
 * every run is identical). Requests are served in the order they arrive until the work runs out.
 */
public class TickServer {

    /** How many requests each client had served and refused in one tick. */
    public record Result(Map<String, Integer> served, Map<String, Integer> refused) {
        public int totalServed() {
            return served.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private final int capacity;

    public TickServer(int capacity) {
        this.capacity = capacity;
    }

    public Result serve(List<Request> arrivals, RateLimiter limiter) {
        Map<String, Integer> served = new LinkedHashMap<>();
        Map<String, Integer> refused = new LinkedHashMap<>();
        int used = 0;
        for (Request request : arrivals) {
            boolean allowed = limiter == null || limiter.allow(request.client());
            if (allowed && used < capacity) {
                used++;
                served.merge(request.client(), 1, Integer::sum);
            } else {
                refused.merge(request.client(), 1, Integer::sum);
            }
        }
        return new Result(served, refused);
    }
}
