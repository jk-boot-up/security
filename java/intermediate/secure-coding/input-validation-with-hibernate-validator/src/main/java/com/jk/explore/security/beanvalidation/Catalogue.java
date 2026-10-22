package com.jk.explore.security.beanvalidation;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** The shop's catalogue: the only place a price comes from. Every lookup is counted, because a lookup is what makes a check expensive. */
public final class Catalogue {

    private final Map<String, BigDecimal> prices = Map.of("SHO-1042", new BigDecimal("89.90"), "BAG-2210", new BigDecimal("45.00"));
    private final AtomicInteger lookups = new AtomicInteger();

    public boolean contains(String sku) {
        lookups.incrementAndGet();
        return prices.containsKey(sku);
    }

    public BigDecimal price(String sku) {
        return prices.get(sku);
    }

    public int lookups() {
        return lookups.get();
    }

    public void reset() {
        lookups.set(0);
    }
}
