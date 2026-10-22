package com.jk.explore.security.failclosedspring;

/** A card payment as the customer's app sends it. The amount is in cents. */
record Payment(String card, String merchant, long amountCents) {
}
