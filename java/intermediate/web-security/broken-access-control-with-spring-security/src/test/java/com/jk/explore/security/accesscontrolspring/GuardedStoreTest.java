package com.jk.explore.security.accesscontrolspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The guarded store over real HTTP: deny by default, object-level checks in the query, method security and the denial log. */
class GuardedStoreTest {

    private static AccessControlSpringDemo.Running shop;

    @BeforeAll
    static void start() {
        shop = AccessControlSpringDemo.start("guarded");
    }

    @AfterAll
    static void stop() {
        shop.close();
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /menu, , 200",
            "GET, /account, , 401",
            "GET, /account, ana, 200",
            "GET, /orders/1001, ana, 200",
            "GET, /orders/1002, ana, 404",
            "GET, /orders/9999, ana, 404",
            "GET, /orders/1002, sam, 200",
            "GET, /orders/1002, dave, 200",
            "DELETE, /orders/1002, ana, 404",
            "GET, /admin/reports, , 401",
            "GET, /admin/reports, ana, 403",
            "GET, /admin/reports, sam, 403",
            "GET, /admin/reports, dave, 200",
            "POST, /admin/refunds?order=1002, ana, 403",
            "POST, /admin/refunds?order=1002, sam, 200",
            "GET, /admin/users, ana, 403",
            "DELETE, /admin/users/cara, sam, 403",
            "PUT, /orders/1001, ana, 403",
            "GET, /admin/debug, dave, 403",
            "PATCH, /admin/users, dave, 403",
    })
    void eachRequestGetsItsStatus(String method, String path, String user, int status) throws Exception {
        assertEquals(status, shop.status(method, path, user));
    }

    @Test
    void attacksAllFailAndHonestRequestsAllWork() throws Exception {
        for (AccessControlSpringDemo.Attempt attack : AccessControlSpringDemo.ATTACKS) {
            assertTrue(shop.status(attack) != 200, attack.label());
        }
        for (AccessControlSpringDemo.Attempt request : AccessControlSpringDemo.HONEST) {
            assertEquals(200, shop.status(request), request.label());
        }
    }

    @Test
    void postAuthorizeRunsTheMethodAndThenRefuses() throws Exception {
        OrderAccess access = shop.bean(OrderAccess.class);
        int before = access.byLinkCalls();
        assertEquals(403, shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ana"));
        assertEquals(before + 1, access.byLinkCalls());
        assertEquals(200, shop.status("GET", "/orders/by-link/" + Catalogue.GIFT_ORDER, "ben"));
    }

    @Test
    void refusalsAreLoggedWithoutCredentials() throws Exception {
        shop.status("DELETE", "/admin/users/ben", "ana");
        shop.status("GET", "/orders/1004", "ana");
        var log = shop.bean(DenialLog.class).lines();
        assertTrue(log.contains("DENY ana DELETE /admin/users/ben"));
        assertTrue(log.contains("DENY ana GET /orders/{id}: order 1004 is not theirs"));
        assertFalse(log.stream().anyMatch(l -> l.contains(Catalogue.PASSWORD) || l.contains("Basic ")));
    }

    @Test
    void everyRouteTheControllersServeHasARule() {
        assertEquals(java.util.List.of(), RouteInventory.withoutRule(shop.context()));
        assertEquals(AccessRules.RULES.size(), RouteInventory.routes(shop.context()).size());
    }
}
