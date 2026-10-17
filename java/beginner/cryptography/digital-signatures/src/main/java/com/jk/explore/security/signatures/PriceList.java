package com.jk.explore.security.signatures;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * The online store's price list: a version, the last day it may be used, and a price in dollars for each product. It is turned
 * into bytes in one fixed way (products sorted by name, one per line), so the same list always gives the same bytes and so
 * the same signature, however it was built.
 */
public record PriceList(int version, int validUntilDay, Map<String, Integer> pricesDollars) {

    public byte[] encode() {
        StringBuilder text = new StringBuilder();
        text.append("version=").append(version).append('\n');
        text.append("validUntilDay=").append(validUntilDay).append('\n');
        new TreeMap<>(pricesDollars).forEach((name, price) -> text.append(name).append('=').append(price).append('\n'));
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
}
