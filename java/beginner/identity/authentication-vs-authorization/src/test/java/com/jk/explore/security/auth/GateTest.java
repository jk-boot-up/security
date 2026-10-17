package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GateTest {

    private final AuthDemo.World w = new AuthDemo.World();

    @Test
    void noGateAllowsEverythingToEveryone() {
        Shop shop = w.shop(new NoGate());
        assertEquals(Status.OK, shop.handle(null, Action.REFUND, "O-1").status());
    }

    @Test
    void loginOnlyLetsAnyLoggedInUserDoAnything() {
        Shop shop = w.shop(new LoginOnlyGate(w.authenticator));
        String ben = w.login("ben");
        assertEquals(Status.OK, shop.handle(ben, Action.REFUND, "O-1").status());
        assertEquals(Status.OK, shop.handle(ben, Action.VIEW, "O-1").status());
        assertEquals(Status.UNAUTHENTICATED, shop.handle(null, Action.VIEW, "O-1").status());
    }

    @Test
    void policyAnswers401WhenNobodyIsKnownAnd403WhenTheUserMayNot() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String ben = w.login("ben");
        assertEquals(401, shop.handle(null, Action.REFUND, "O-1").status().code());
        assertEquals(403, shop.handle(ben, Action.REFUND, "O-1").status().code());
        assertEquals(200, shop.handle(ben, Action.VIEW, "O-2").status().code());
    }

    @Test
    void anExpiredSessionIsUnauthenticatedNotForbidden() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String sam = w.login("sam");
        w.ticker.advance(AuthDemo.SESSION_LIFETIME + 1);
        assertEquals(Status.UNAUTHENTICATED, shop.handle(sam, Action.REFUND, "O-1").status());
    }
}
