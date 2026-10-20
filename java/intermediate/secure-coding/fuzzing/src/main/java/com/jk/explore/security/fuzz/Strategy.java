package com.jk.explore.security.fuzz;

/** How the next input is made. */
public enum Strategy {
    /** Random printable text, with no idea what a valid file looks like. */
    RANDOM_TEXT,
    /** Valid example files, changed a little each time. */
    MUTATE_SEEDS,
    /** Valid example files again, but any input that reaches a branch nobody has reached is kept and mutated in turn. */
    COVERAGE_GUIDED
}
