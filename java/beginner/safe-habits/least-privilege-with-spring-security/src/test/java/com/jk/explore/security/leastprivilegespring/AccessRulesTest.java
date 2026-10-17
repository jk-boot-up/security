package com.jk.explore.security.leastprivilegespring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

/** The rules of the store, checked through the real server and through the secured beans. */
class AccessRulesTest {

    private static LeastPrivilegeSpringDemo.Running shop;

    @BeforeAll
    static void start() {
        shop = LeastPrivilegeSpringDemo.start();
    }

    @AfterAll
    static void stop() {
        shop.close();
    }

    private static int status(String method, String path, String user) throws IOException, InterruptedException {
        return Wire.call(shop.port(), method, path, user).status();
    }

    @Test
    void anonymousIsAskedToLogInAndAWrongPasswordIsRefused() throws Exception {
        assertEquals(401, status("GET", "/orders", null));
        assertEquals(401, Wire.call(shop.port(), "GET", "/orders", "alice", "guess").status());
    }

    @Test
    void everyAccountCanDoExactlyWhatItWasGiven() throws Exception {
        assertEquals(200, status("GET", "/orders", "alice"));
        assertEquals(403, status("GET", "/reports", "alice"));
        assertEquals(200, status("GET", "/reports", "carol"));
        assertEquals(200, status("GET", "/orders", "exporter"));
        assertEquals(403, status("GET", "/secrets", "exporter"));
        assertEquals(200, status("GET", "/secrets", "dave"));
    }

    @Test
    void unlistedPathsAreDeniedEvenForAdmins() throws Exception {
        assertEquals(403, status("GET", "/admin/debug", "dave"));
        assertEquals(403, status("DELETE", "/orders", "dave"));
        assertEquals(403, status("GET", "/actuator/env", "dave"));
    }

    @Test
    void roleHierarchyLetsAdminActAsSupportButNotFinanceAsSupport() throws Exception {
        assertEquals(200, status("GET", "/staff/queue", "dave"));
        assertEquals(200, status("GET", "/staff/queue", "alice"));
        assertEquals(403, status("GET", "/staff/queue", "carol"));
        assertEquals(403, status("GET", "/staff/queue", "exporter"));
    }

    @Test
    void blastRadiusFollowsTheGrants() {
        assertEquals(15, BlastRadius.reach(Catalogue.ACCOUNTS.get("dave")));
        assertEquals(2, BlastRadius.reach(Catalogue.ACCOUNTS.get("exporter")));
        assertEquals(3, BlastRadius.reach(Catalogue.ACCOUNTS.get("alice")));
    }

    @Test
    void nobodyApprovesTheirOwnRefundNotEvenAnAdmin() {
        Refunds refunds = shop.bean(Refunds.class);
        try {
            SecurityContextHolder.getContext().setAuthentication(BlastRadius.token(Catalogue.ACCOUNTS.get("dave")));
            String id = refunds.create().id();
            assertThrows(AccessDeniedException.class, () -> refunds.approve(id));
            SecurityContextHolder.getContext().setAuthentication(BlastRadius.token(Catalogue.ACCOUNTS.get("carol")));
            assertEquals("carol", refunds.approve(id).approver());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void approvingNothingIsDeniedNotACrash() {
        Refunds refunds = shop.bean(Refunds.class);
        try {
            SecurityContextHolder.getContext().setAuthentication(BlastRadius.token(Catalogue.ACCOUNTS.get("carol")));
            assertThrows(AccessDeniedException.class, () -> refunds.approve("R-999"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void createNeedsThePermissionNotJustTheRole() {
        Refunds refunds = shop.bean(Refunds.class);
        try {
            SecurityContextHolder.getContext().setAuthentication(BlastRadius.token(Catalogue.ACCOUNTS.get("bob")));
            assertThrows(AccessDeniedException.class, refunds::create);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void grantsOpenAndCloseWithTheClock() throws Exception {
        SimulatedClock clock = shop.bean(SimulatedClock.class);
        Grants grants = shop.bean(Grants.class);
        grants.give("bob", "payments:write", 100, 102);
        clock.set(99);
        assertEquals(403, status("PUT", "/payments/P-9", "bob"));
        clock.set(100);
        assertEquals(200, status("PUT", "/payments/P-9", "bob"));
        clock.set(102);
        assertEquals(200, status("PUT", "/payments/P-9", "bob"));
        clock.set(103);
        assertEquals(403, status("PUT", "/payments/P-9", "bob"));
        assertTrue(grants.active("bob", "payments:write") == false);
        clock.set(0);
    }

    @Test
    void aGrantBelongsToOnePersonAndOnePermission() {
        Grants grants = new Grants(new SimulatedClock());
        grants.give("carol", "payments:write", 0, 0);
        assertTrue(grants.active("carol", "payments:write"));
        assertTrue(!grants.active("alice", "payments:write"));
        assertTrue(!grants.active("carol", "payments:read"));
    }
}
