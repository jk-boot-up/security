package com.jk.explore.security.leastprivilege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessTest {

    @Test
    void aUserWithNoRolesCanDoNothing() {
        assertFalse(new Access().can("nobody", "orders:read", 0));
    }

    @Test
    void rolesAddUp() {
        Access access = new Access().assign("x", "support", "warehouse");
        assertTrue(access.can("x", "orders:read", 0));
        assertTrue(access.can("x", "inventory:write", 0));
        assertFalse(access.can("x", "payments:read", 0));
    }

    @Test
    void aUserDenyBeatsARoleAllow() {
        Access access = new Access().assign("x", "admin").restrict("x", Policy.of("deny secrets:*"));
        assertFalse(access.can("x", "secrets:read", 0));
        assertTrue(access.can("x", "orders:delete", 0));
    }

    @Test
    void grantsAreActiveOnlyBetweenFromAndUntil() {
        Access access = new Access().assign("x", "finance");
        access.grant("x", "payments:write", 10, 13);
        assertFalse(access.can("x", "payments:write", 9));
        assertTrue(access.can("x", "payments:write", 10));
        assertTrue(access.can("x", "payments:write", 12));
        assertFalse(access.can("x", "payments:write", 13));
    }

    @Test
    void aGrantDoesNotBeatAnExplicitDeny() {
        Access access = new Access().assign("x", "support").restrict("x", Policy.of("deny payments:*"));
        access.grant("x", "payments:write", 0, 100);
        assertFalse(access.can("x", "payments:write", 5));
    }

    @Test
    void aGrantOnlyCoversWhatItNames() {
        Access access = new Access();
        access.grant("x", "payments:write", 0, 100);
        assertFalse(access.can("x", "payments:read", 5));
    }

    @Test
    void refundsNeedTwoDifferentPeople() {
        Access access = new Access().assign("alice", "support").assign("carol", "finance").assign("dave", "admin");
        RefundDesk desk = new RefundDesk(access);
        assertEquals(RefundDesk.Outcome.UNKNOWN_REFUND, desk.approve("R", "carol", 0));
        assertEquals(RefundDesk.Outcome.DONE, desk.create("R", "alice", 0));
        assertEquals(RefundDesk.Outcome.NOT_ALLOWED, desk.approve("R", "alice", 0));
        assertFalse(desk.isApproved("R"));
        assertEquals(RefundDesk.Outcome.DONE, desk.approve("R", "carol", 0));
        assertTrue(desk.isApproved("R"));
    }

    @Test
    void anAdminCannotApproveTheirOwnRefund() {
        Access access = new Access().assign("dave", "admin");
        RefundDesk desk = new RefundDesk(access);
        assertEquals(RefundDesk.Outcome.DONE, desk.create("R", "dave", 0));
        assertEquals(RefundDesk.Outcome.SAME_PERSON, desk.approve("R", "dave", 0));
        assertFalse(desk.isApproved("R"));
    }

    @Test
    void someoneWithoutThePermissionCannotCreate() {
        assertEquals(RefundDesk.Outcome.NOT_ALLOWED, new RefundDesk(new Access().assign("bob", "warehouse")).create("R", "bob", 0));
    }
}
