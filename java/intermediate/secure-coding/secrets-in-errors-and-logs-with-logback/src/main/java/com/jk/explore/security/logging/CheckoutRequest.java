package com.jk.explore.security.logging;

/** What the checkout page posts, with the sensitive parts in types that cannot be printed by accident. */
public record CheckoutRequest(String name, Card card, Secret cvv, int order) {
}
