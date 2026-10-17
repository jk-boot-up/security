package com.jk.explore.security.accesscontrolspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The ownership check lives in the lookup itself. */
class OrdersTest {

    @Test
    void findByIdAndOwnerFindsOnlyTheOwnersOrder() {
        Orders orders = new Orders();
        assertEquals("ana", orders.findByIdAndOwner("1001", "ana").orElseThrow().getOwner());
        assertTrue(orders.findByIdAndOwner("1002", "ana").isEmpty());
        assertTrue(orders.findById("1002").isPresent());
    }

    @Test
    void someoneElsesOrderLooksExactlyLikeAMissingOne() {
        Orders orders = new Orders();
        assertEquals(orders.findByIdAndOwner("9999", "ana"), orders.findByIdAndOwner("1002", "ana"));
    }

    @Test
    void everyRuleHasAMethodAndAPathAndNoneIsListedTwice() {
        assertEquals(AccessRules.RULES.size(), AccessRules.RULES.stream().map(r -> r.method() + " " + r.pattern()).distinct().count());
        assertFalse(AccessRules.covers("GET", "/admin/export"));
        assertTrue(AccessRules.covers("DELETE", "/admin/users/{name}"));
    }
}
