package com.jk.explore.security.signatures;

import java.security.PrivateKey;

/** The shop, which holds the private key and signs each price list it publishes. */
public class Publisher {

    public static final String CONTEXT = "shop.price-list.v1";

    private final PrivateKey key;

    public Publisher(PrivateKey key) {
        this.key = key;
    }

    public Signed publish(PriceList list) {
        return new Signed(list, Signing.sign(key, CONTEXT, list.encode()));
    }
}
