package com.jk.explore.security.baosecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The teaching policy check needs no running OpenBao; the demo's golden output shows it agrees with the real server. */
class PolicyRuleTest {

    private final List<PolicyRule.Rule> shop = PolicyRule.parse(BaoSecretsDemo.policy("shop-api"));

    @Test
    void theStorePolicyHasTwoReadRules() {
        assertEquals(2, shop.size());
        assertEquals("secret/data/shop-api/*", shop.get(0).path());
        assertEquals(java.util.Set.of("read"), shop.get(1).capabilities());
    }

    @Test
    void aStarCoversEveryPathBelowIt() {
        assertTrue(PolicyRule.allows(shop, "secret/data/shop-api/payment", "read"));
        assertTrue(PolicyRule.allows(shop, "secret/data/shop-api/a/b", "read"));
        assertFalse(PolicyRule.allows(shop, "secret/data/reports/warehouse", "read"));
    }

    @Test
    void aPathWithoutAStarCoversOnlyItself() {
        assertTrue(PolicyRule.allows(shop, "database/creds/shop-api", "read"));
        assertFalse(PolicyRule.allows(shop, "database/creds/shop-api-2", "read"));
        assertFalse(PolicyRule.allows(shop, "database/creds/reports", "read"));
    }

    @Test
    void aCapabilityTheRuleDoesNotNameIsRefused() {
        assertFalse(PolicyRule.allows(shop, "secret/data/shop-api/payment", "update"));
        assertFalse(PolicyRule.allows(List.of(), "secret/data/shop-api/payment", "read"));
    }

    @Test
    void theMostSpecificRuleDecides() {
        List<PolicyRule.Rule> rules = PolicyRule.parse("path \"secret/*\" { capabilities = [\"read\"] }\n"
                + "path \"secret/data/admin\" { capabilities = [\"deny\"] }");
        assertTrue(PolicyRule.allows(rules, "secret/data/other", "read"));
        assertFalse(PolicyRule.allows(rules, "secret/data/admin", "read"));
    }
}
