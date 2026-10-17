package com.jk.explore.security.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Each weakness of the first version, pinned down so the lesson cannot quietly change. */
class NaiveStoreTest {

    private final ShopData data = ShopData.sample();
    private final NaiveStore store = new NaiveStore(data);

    @Test
    void anySignedInCustomerReadsAnyOrder() {
        assertEquals(200, store.handle(Request.of("GET", "/orders/1002", Attempts.ANA)).status());
        assertEquals(200, store.handle(Request.of("GET", "/orders/" + ShopData.GIFT_ORDER, Attempts.ANA)).status());
    }

    @Test
    void signedOutReadersAreStillAskedToSignIn() {
        assertEquals(401, store.handle(Request.of("GET", "/orders/1002", null)).status());
    }

    @Test
    void theForgottenReportIsOpenToEveryone() {
        assertEquals(200, store.handle(Request.of("GET", "/admin/reports", null)).status());
    }

    @Test
    void theRefundIsOpenToCustomersAlthoughThePageHidesIt() {
        assertFalse(store.handle(Request.of("GET", "/menu", Attempts.ANA)).body().contains("admin"));
        assertEquals(200, store.handle(Request.of("POST", "/admin/refunds", Attempts.ANA, "1002")).status());
        assertTrue(data.refunds().contains("1002"));
    }

    @Test
    void getIsCheckedButDeleteIsNot() {
        assertEquals(403, store.handle(Request.of("GET", "/admin/users", Attempts.ANA)).status());
        assertEquals(200, store.handle(Request.of("DELETE", "/admin/users/ben", Attempts.ANA)).status());
        assertTrue(data.userFor(Attempts.BEN).isEmpty());
    }

    @Test
    void anotherCustomersAddressCanBeChanged() {
        store.handle(Request.of("POST", "/orders/1002/address", Attempts.ANA, "12 Palm Street"));
        assertEquals("12 Palm Street", data.order("1002").orElseThrow().address());
    }

    @Test
    void theCarefulRouteRefusesAnotherCustomer() {
        assertEquals(403, store.handle(Request.of("DELETE", "/orders/1002", Attempts.ANA)).status());
        assertEquals(200, store.handle(Request.of("DELETE", "/orders/1002", Attempts.BEN)).status());
    }

    @Test
    void sevenOfTheNineAttacksSucceed() {
        long ok = Attempts.attacks().stream().filter(a -> new NaiveStore(ShopData.sample()).handle(a.request()).succeeded()).count();
        assertEquals(7, ok);
    }
}
