package com.jk.explore.security.beanvalidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** The service that takes an order. The constraints on its parameter are the boundary: nothing past it sees an unchecked form. */
public class Orders {

    private final Catalogue catalogue;

    public Orders(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    /** The total from the catalogue's prices. */
    public BigDecimal place(@NotNull @Valid Checkout checkout) {
        return total(checkout);
    }

    /** The same calculation, with no constraints on the way in: the shop that trusts the page. */
    public BigDecimal placeUnchecked(Checkout checkout) {
        return total(checkout);
    }

    private BigDecimal total(Checkout checkout) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Line line : checkout.lines()) {
            sum = sum.add(catalogue.price(line.sku()).multiply(BigDecimal.valueOf(line.quantity())));
        }
        return sum;
    }
}
