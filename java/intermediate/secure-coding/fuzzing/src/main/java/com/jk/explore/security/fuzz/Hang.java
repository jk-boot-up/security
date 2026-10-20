package com.jk.explore.security.fuzz;

/** Thrown by {@link Coverage} when a run has taken too many steps: a deterministic stand-in for a timeout. */
public class Hang extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Hang() {
        super("step budget exceeded");
    }
}
