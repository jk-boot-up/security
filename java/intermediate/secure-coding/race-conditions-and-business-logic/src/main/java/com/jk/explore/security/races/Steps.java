package com.jk.explore.security.races;

/**
 * A request broken into the separate steps a server performs, so that two requests can be interleaved in a chosen order.
 * A real server runs these steps on threads, and the operating system picks the order; here the order is written down, so the
 * outcome is the same on every run.
 */
interface Steps {

    /** The request's short name in the trace, such as "A". */
    String name();

    /** Runs the next step and describes it. A step that cannot run yet (it waits for a lock) leaves {@link #waiting()} true. */
    String step();

    /** Whether the last step was spent waiting, without progress. */
    boolean waiting();

    boolean finished();

    /** What the request ended with, such as "paid out 80" or "refused". */
    String outcome();
}
