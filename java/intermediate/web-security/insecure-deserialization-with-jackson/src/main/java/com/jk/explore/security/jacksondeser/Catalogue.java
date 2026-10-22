package com.jk.explore.security.jacksondeser;

import java.util.Map;

/** The shop's own prices, which are the only ones that count. */
public final class Catalogue {

    private static final Map<String, Long> PRICES = Map.of("KTL-001", 2500L, "PAN-002", 4200L);

    private Catalogue() {
    }

    public static long price(String sku) throws Refused {
        Long price = PRICES.get(sku);
        if (price == null) {
            throw new Refused("unknown product");
        }
        return price;
    }
}
