package com.jk.explore.security.accesscontrolspring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The naive store's three mistakes, and a later route without a rule. */
class NaiveAndLaterStoreTest {

    @Test
    void theNaiveChainLetsTheThreeMistakesThrough() throws Exception {
        try (AccessControlSpringDemo.Running shop = AccessControlSpringDemo.start("naive")) {
            assertEquals(200, shop.status("GET", "/orders/1002", "ana"), "signed in is not ownership");
            assertEquals(200, shop.status("GET", "/admin/reports", null), "permitAll at the end");
            assertEquals(403, shop.status("GET", "/admin/users", "ana"));
            assertEquals(200, shop.status("DELETE", "/admin/users/cara", "ana"), "the pattern does not cover the sub-path");
        }
    }

    @Test
    void aNewRouteWithoutARuleIsReportedAndRefused() throws Exception {
        try (AccessControlSpringDemo.Running shop = AccessControlSpringDemo.start("guarded", "with-export")) {
            assertEquals(List.of("GET /admin/export"), RouteInventory.withoutRule(shop.context()));
            assertEquals(403, shop.status("GET", "/admin/export", "dave"));
        }
    }
}
