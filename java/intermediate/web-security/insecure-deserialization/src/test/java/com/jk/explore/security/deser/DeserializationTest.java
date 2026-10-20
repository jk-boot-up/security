package com.jk.explore.security.deser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeserializationTest {

    @TempDir
    Path dir;

    private final Cart honest = new Cart().add("KTL-001", 2, 2500).add("PAN-002", 1, 4200);

    @Test
    void anUnfilteredReaderRunsTheGadgetBeforeItNoticesTheCookieIsNotACart() throws Exception {
        Path marker = Files.writeString(dir.resolve("marker"), "x");
        var e = assertThrows(CartRefused.class, () -> SerializedCartCookie.unfiltered().read(Attacker.cookieFor(new TempCleaner(marker.toString()))));
        assertEquals("the cookie is not a cart", e.getMessage());
        assertFalse(Files.exists(marker), "readObject already ran");
    }

    @Test
    void anUnfilteredReaderTrustsThePriceInTheCookie() {
        CartCookie c = SerializedCartCookie.unfiltered();
        assertEquals(2, c.read(Attacker.cookieFor(new Cart().add("KTL-001", 2, 1))).totalCents());
    }

    @Test
    void theAllowListRefusesTheGadgetBeforeItIsBuilt() throws Exception {
        Path marker = Files.writeString(dir.resolve("marker"), "x");
        assertThrows(CartRefused.class, () -> SerializedCartCookie.withAllowList().read(Attacker.cookieFor(new TempCleaner(marker.toString()))));
        assertTrue(Files.exists(marker));
    }

    @Test
    void theAllowListStillReadsHonestCartsButNotOtherClasses() {
        CartCookie c = SerializedCartCookie.withAllowList();
        assertEquals(9200, c.read(c.write(honest)).totalCents());
        assertThrows(CartRefused.class, () -> c.read(Attacker.cookieFor(new java.util.HashMap<String, String>())));
        assertThrows(CartRefused.class, () -> c.read(Attacker.cookieFor(java.net.URI.create("http://x.example"))));
    }

    @Test
    void theAllowListLimitsSizeAndTheFilterDoesNotProtectPrices() {
        CartCookie c = SerializedCartCookie.withAllowList();
        Cart big = new Cart();
        for (int i = 0; i < 150; i++) {
            big.add("KTL-001", 1, 1);
        }
        assertThrows(CartRefused.class, () -> c.read(c.write(big)));
        assertEquals(2, c.read(Attacker.cookieFor(new Cart().add("KTL-001", 2, 1))).totalCents());
    }

    @Test
    void garbageIsRefused() {
        assertThrows(CartRefused.class, () -> SerializedCartCookie.withAllowList().read("!!not base64!!"));
        assertThrows(CartRefused.class, () -> SerializedCartCookie.withAllowList().read("AAAA"));
    }

    @Test
    void signedTextRoundTripsAndPricesComeFromTheServer() {
        CartCookie c = new SignedTextCartCookie(DeserializationDemo.DEMO_KEY, DeserializationDemo.PRICES);
        Cart cart = c.read(c.write(new Cart().add("KTL-001", 2, 1)));
        assertEquals(2 * 2500, cart.totalCents());
    }

    @Test
    void signedTextRefusesChangesObjectsAndForeignKeys() {
        CartCookie c = new SignedTextCartCookie(DeserializationDemo.DEMO_KEY, DeserializationDemo.PRICES);
        String cookie = c.write(honest);
        assertEquals("bad signature", assertThrows(CartRefused.class, () -> c.read(DeserializationDemo.tamper(cookie))).getMessage());
        assertThrows(CartRefused.class, () -> c.read(Attacker.cookieFor(honest)));
        CartCookie other = new SignedTextCartCookie("another-key-another-key".getBytes(), DeserializationDemo.PRICES);
        assertThrows(CartRefused.class, () -> c.read(other.write(honest)));
        assertThrows(CartRefused.class, () -> c.read("a.b.c"));
    }

    @Test
    void signedTextChecksTheShapeOfEachLine() {
        CartCookie c = new SignedTextCartCookie(DeserializationDemo.DEMO_KEY, DeserializationDemo.PRICES);
        assertThrows(CartRefused.class, () -> c.read(c.write(new Cart().add("bad sku", 1, 1))));
        assertThrows(CartRefused.class, () -> c.read(c.write(new Cart().add("KTL-001", 0, 1))));
    }
}
