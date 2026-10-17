package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HmacContrastTest {

    @Test
    void anyHolderOfTheSharedKeyCanForgeATheOtherSideAccepts() {
        byte[] key = Hex.bytes("k");
        HmacPublisher shop = new HmacPublisher(key);
        HmacPublisher warehouse = new HmacPublisher(key);
        PriceList forged = SignaturesDemo.list(3, 100, 12, 25);
        assertTrue(shop.accepts(forged, warehouse.tag(forged)));
    }
}
