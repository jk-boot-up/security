package com.jk.explore.security.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The test that keeps the policy complete: it fails the build when a route is added without a rule. */
class RouteInventoryTest {

    @Test
    void everyRouteOfTheStoreHasARule() {
        assertEquals(List.of(), new GuardedStore(ShopData.sample()).routesWithoutRules());
    }

    @Test
    void aForgottenRuleIsReported() {
        GuardedStore store = new GuardedStore(ShopData.sample()).addRouteWithoutRule("GET", "/admin/export", Handlers::exportCustomers);
        assertEquals(List.of(new Route("GET", "/admin/export")), store.routesWithoutRules());
    }

    @Test
    void routesMatchWholeSegmentsOnly() {
        Route route = new Route("GET", "/orders/{id}");
        assertEquals("1002", route.match(Request.of("GET", "/orders/1002", null)).orElseThrow().get("id"));
        assertTrue(route.match(Request.of("GET", "/orders/", null)).isEmpty());
        assertTrue(route.match(Request.of("GET", "/orders/1002/address", null)).isEmpty());
        assertTrue(route.match(Request.of("DELETE", "/orders/1002", null)).isEmpty());
    }
}
