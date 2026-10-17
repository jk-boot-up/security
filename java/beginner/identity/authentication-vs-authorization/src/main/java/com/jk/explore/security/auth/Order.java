package com.jk.explore.security.auth;

/** One order in the store: who owns it, what it is, and what was paid. */
public record Order(String id, String owner, String item, int amountDollars) {
}
