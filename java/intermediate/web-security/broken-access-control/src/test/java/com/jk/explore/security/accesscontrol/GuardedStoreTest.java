package com.jk.explore.security.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The fixed store: every attack refused, every honest request still served, every refusal logged without secrets. */
class GuardedStoreTest {

    static java.util.List<Attempts.Attempt> attacks() {
        return Attempts.attacks();
    }

    static java.util.List<Attempts.Attempt> honest() {
        return Attempts.honest();
    }

    @ParameterizedTest
    @MethodSource("attacks")
    void everyAttackIsRefused(Attempts.Attempt attempt) {
        assertFalse(new GuardedStore(ShopData.sample()).handle(attempt.request()).succeeded(), attempt.label());
    }

    @ParameterizedTest
    @MethodSource("honest")
    void everyHonestRequestStillWorks(Attempts.Attempt attempt) {
        assertTrue(new GuardedStore(ShopData.sample()).handle(attempt.request()).succeeded(), attempt.label());
    }

    @Test
    void someoneElsesOrderLooksLikeAMissingOne() {
        GuardedStore store = new GuardedStore(ShopData.sample());
        assertEquals(404, store.handle(Request.of("GET", "/orders/1002", Attempts.ANA)).status());
        assertEquals(404, store.handle(Request.of("GET", "/orders/9999", Attempts.ANA)).status());
    }

    @Test
    void signedOutGetsUnauthorizedAndTheWrongRoleGetsForbidden() {
        GuardedStore store = new GuardedStore(ShopData.sample());
        assertEquals(401, store.handle(Request.of("GET", "/admin/reports", null)).status());
        assertEquals(403, store.handle(Request.of("GET", "/admin/reports", Attempts.ANA)).status());
        assertEquals(401, store.handle(Request.of("GET", "/account", "token-guessed")).status());
    }

    @Test
    void supportMayReadButNotChangeAnOrder() {
        GuardedStore store = new GuardedStore(ShopData.sample());
        assertEquals(200, store.handle(Request.of("GET", "/orders/1002", Attempts.SAM)).status());
        assertEquals(404, store.handle(Request.of("POST", "/orders/1002/address", Attempts.SAM, "x")).status());
        assertEquals(403, store.handle(Request.of("POST", "/admin/refunds", Attempts.SAM, "1002")).status());
    }

    @Test
    void refusalsAreLoggedWithWhoAndWhyButNoToken() {
        GuardedStore store = new GuardedStore(ShopData.sample());
        for (Attempts.Attempt attempt : Attempts.attacks()) {
            store.handle(attempt.request());
        }
        assertEquals(Attempts.attacks().size(), store.denials().size());
        assertTrue(store.denials().getFirst().startsWith("DENY ana GET /orders/{id}"));
        assertTrue(store.denials().stream().noneMatch(l -> l.contains("token-")));
    }

    @Test
    void aRouteWithoutARuleIsRefusedEvenToAnAdmin() {
        GuardedStore store = new GuardedStore(ShopData.sample()).addRouteWithoutRule("GET", "/admin/export", Handlers::exportCustomers);
        assertEquals(403, store.handle(Request.of("GET", "/admin/export", Attempts.DAVE)).status());
    }

    @Test
    void unknownPathsAndMethodsAreNotFound() {
        GuardedStore store = new GuardedStore(ShopData.sample());
        assertEquals(404, store.handle(Request.of("PUT", "/orders/1001", Attempts.ANA)).status());
        assertEquals(404, store.handle(Request.of("GET", "/admin/reports/", Attempts.DAVE)).status());
    }
}
