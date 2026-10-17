package com.jk.explore.security.signatures;

/** A message together with its signature, as the shop publishes it. */
public record Signed(PriceList priceList, byte[] signature) {
}
