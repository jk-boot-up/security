package com.jk.explore.security.validation;

import java.math.BigDecimal;
import java.util.Map;

/** The checkout that trusts what arrives because the page already checked it, including the price in a hidden field. */
public final class TrustingShop {

    private TrustingShop() {
    }

    /** The order total the shop would charge. */
    public static BigDecimal total(Map<String, String> fields) {
        int quantity = Integer.parseInt(fields.get("quantity"));
        return price(fields).multiply(BigDecimal.valueOf(quantity));
    }

    /** The unit price it uses: the form's hidden price field if there is one, else the catalogue's. */
    public static BigDecimal price(Map<String, String> fields) {
        return new BigDecimal(fields.getOrDefault("price",
                Catalogue.price(fields.get("sku")).orElse(BigDecimal.ZERO).toPlainString()));
    }
}
