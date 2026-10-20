package com.jk.explore.security.fuzzing;

/** An amount of money in cents. It can never be negative: a negative total would be a refund nobody asked for. */
record Money(long cents) {

    Money {
        if (cents < 0) {
            throw new IllegalArgumentException("a total cannot be negative");
        }
    }
}
