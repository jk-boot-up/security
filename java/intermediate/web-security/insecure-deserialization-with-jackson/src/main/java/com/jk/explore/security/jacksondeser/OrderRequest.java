package com.jk.explore.security.jacksondeser;

import java.util.List;

/** What the customer may say about a cart: what and how many. The price is the shop's to decide, so it is not in the request at all. */
public record OrderRequest(List<Item> items) {

    public record Item(String sku, int qty) {
    }
}
