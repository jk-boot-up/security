package com.jk.explore.security.fuzz;

/** One parsed product line: what, how many, and the total in cents. */
public record OrderLine(String sku, int quantity, int unit, int totalCents) {
}
