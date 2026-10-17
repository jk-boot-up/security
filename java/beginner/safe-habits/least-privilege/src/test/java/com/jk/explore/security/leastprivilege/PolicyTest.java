package com.jk.explore.security.leastprivilege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolicyTest {

    @ParameterizedTest
    @CsvSource({"*,orders:read,true", "orders:*,orders:read,true", "*:read,orders:read,true", "*:read,orders:write,false",
            "orders:read,orders:read,true", "orders:read,orders:readx,false", "orders:read,customers:read,false",
            "orders,orders:read,false", "orders:read,orders,false"})
    void patternsMatch(String pattern, String permission, boolean expected) {
        assertEquals(expected, Policy.matches(pattern, permission));
    }

    @Test
    void nothingIsAllowedByDefault() {
        assertEquals(Policy.Decision.DENY_DEFAULT, Policy.of().decide("orders:read"));
        assertEquals(Policy.Decision.DENY_DEFAULT, Policy.of("allow orders:read").decide("orders:write"));
    }

    @Test
    void anExplicitDenyBeatsAnAllowWhateverTheOrder() {
        assertEquals(Policy.Decision.DENY_EXPLICIT, Policy.of("allow *", "deny secrets:*").decide("secrets:read"));
        assertEquals(Policy.Decision.DENY_EXPLICIT, Policy.of("deny secrets:*", "allow *").decide("secrets:read"));
        assertTrue(Policy.of("allow *", "deny secrets:*").allows("orders:read"));
    }

    @Test
    void badStatementsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Policy.of("permit orders:read"));
        assertThrows(IllegalArgumentException.class, () -> Policy.of("allow"));
    }

    @Test
    void reachableCountsTheBlastRadius() {
        assertEquals(15, Policy.of("allow *").reachable(Catalogue.PERMISSIONS).size());
        assertEquals(List.of("orders:read", "reports:read"), Catalogue.ROLES.get("exporter").reachable(Catalogue.PERMISSIONS));
        assertEquals(13, Policy.of("allow *", "deny secrets:*").reachable(Catalogue.PERMISSIONS).size());
    }

    @Test
    void noRoleButAdminCanApproveRefundsExceptFinance() {
        for (var role : Catalogue.ROLES.entrySet()) {
            boolean can = role.getValue().allows("refunds:approve");
            assertEquals(role.getKey().equals("admin") || role.getKey().equals("finance"), can, role.getKey());
        }
        assertFalse(Catalogue.ROLES.get("support").allows("refunds:approve"));
    }
}
