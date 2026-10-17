package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StaleRoleTest {

    private final AuthDemo.World w = new AuthDemo.World();

    @Test
    void aRoleCopiedAtLoginStaysAfterADemotion() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String sam = w.login("sam");
        w.directory.changeRole("sam", Role.CUSTOMER);
        assertEquals(Status.OK, shop.handle(sam, Action.REFUND, "O-1").status());
    }

    @Test
    void aRoleLookedUpEachTimeFollowsTheDemotionAtOnce() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.DIRECTORY));
        String sam = w.login("sam");
        w.directory.changeRole("sam", Role.CUSTOMER);
        assertEquals(Status.FORBIDDEN, shop.handle(sam, Action.REFUND, "O-1").status());
    }

    @Test
    void lookingUpEachTimeCostsOneLookupPerRequest() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.DIRECTORY));
        String sam = w.login("sam");
        int before = w.directory.lookups();
        shop.handle(sam, Action.VIEW, "O-1");
        shop.handle(sam, Action.VIEW, "O-2");
        assertEquals(2, w.directory.lookups() - before);
    }

    @Test
    void copyingTheRoleCostsNoLookupsAfterLogin() {
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String sam = w.login("sam");
        int before = w.directory.lookups();
        shop.handle(sam, Action.VIEW, "O-1");
        assertEquals(0, w.directory.lookups() - before);
    }
}
