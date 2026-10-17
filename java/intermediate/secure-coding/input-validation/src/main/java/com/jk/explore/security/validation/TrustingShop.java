package com.jk.explore.security.validation;

import java.math.BigDecimal;
import java.util.Map;

/** The checkout that trusts what arrives because the page already checked it, including the price in a hidden field. */
public final class TrustingShop {

    private TrustingShop() {
    }

    /** The order total the shop would charge. */
    public static BigDecimal total(Map<String, String> fields) {
        BigDecimal price = new BigDecimal(fields.getOrDefault("price",
                Catalogue.price(fields.get("sku")).orElse(BigDecimal.ZERO).toPlainString()));
        int quantity = Integer.parseInt(fields.get("quantity"));
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
