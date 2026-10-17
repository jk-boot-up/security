package com.jk.explore.security.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The same policy, over real HTTP on the loopback address, and the token lookup behind it. */
class HttpFrontTest {

    @Test
    void theGuardedStoreAnswersTheSameOverHttp() throws Exception {
        try (HttpFront front = new HttpFront(new GuardedStore(ShopData.sample()))) {
            assertEquals(200, front.send(Request.of("GET", "/orders/1001", Attempts.ANA)));
            assertEquals(404, front.send(Request.of("GET", "/orders/1002", Attempts.ANA)));
            assertEquals(403, front.send(Request.of("DELETE", "/admin/users/ben", Attempts.ANA)));
            assertEquals(401, front.send(Request.of("GET", "/admin/reports", null)));
            assertEquals(200, front.send(Request.of("POST", "/admin/refunds", Attempts.DAVE, "1001")));
        }
    }

    @Test
    void tokensMustMatchExactly() {
        ShopData data = ShopData.sample();
        assertEquals("ana", data.userFor(Attempts.ANA).orElseThrow().name());
        assertTrue(data.userFor(null).isEmpty());
        assertTrue(data.userFor("TOKEN-ANA-7Q2X").isEmpty());
        assertTrue(data.userFor(Attempts.ANA + " ").isEmpty());
    }
}
