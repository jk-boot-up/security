package com.jk.explore.security.validation;

import java.util.Map;

/**
 * The checks a browser makes before it sends the checkout form: the form's `required`, `min`, `max` and `pattern` attributes.
 * They help honest customers. Anyone can skip them by sending the request without a browser.
 */
public final class BrowserForm {

    private BrowserForm() {
    }

    /** True if a browser would let the customer press "Place order" with these values. */
    public static boolean wouldSubmit(Map<String, String> fields) {
        String quantity = fields.getOrDefault("quantity", "");
        String sku = fields.getOrDefault("sku", "");
        if (!quantity.matches("[0-9]{1,2}") || Integer.parseInt(quantity) < 1) {
            return false;
        }
        return sku.matches("[A-Z]{3}-[0-9]{4}");
    }
}
