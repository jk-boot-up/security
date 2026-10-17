package com.jk.explore.security.validation;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** The store's own prices. A price comes from here, never from the customer's form. */
public final class Catalogue {

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "SHO-1042", new BigDecimal("89.90"),
            "HAT-2001", new BigDecimal("19.50"));

    private Catalogue() {
    }

    public static Optional<BigDecimal> price(Sku sku) {
        return Optional.ofNullable(PRICES.get(sku.value()));
    }

    static Optional<BigDecimal> price(String raw) {
        return Optional.ofNullable(PRICES.get(raw));
    }
}
