package com.jk.explore.security.jacksondeser;

/** One line of a cart: what, how many, and what the cookie says it costs. */
public record Line(String sku, int qty, long price) {
}
