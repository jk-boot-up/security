package com.jk.explore.security.certsbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The name rule by hand must agree with the JDK's PKIX check of the intermediate's name constraint. */
class NameRuleTest {

    @Test
    void theRuleAgreesWithTheJdk() {
        Pki pki = new Pki();
        ChainValidator browser = new ChainValidator(Set.of(pki.root.certificate()));
        long serial = 20;
        for (String host : List.of("shop.example", "api.shop.example", "other.example", "evilshop.example")) {
            boolean jdk = browser.validate(List.of(pki.serverFor(host, serial++).certificate(), pki.intermediate.certificate()), Pki.TODAY).accepted();
            assertEquals(jdk, NameRule.permits("shop.example", host), host);
        }
    }

    @Test
    void theDotMatters() {
        assertTrue(NameRule.permits("shop.example", "a.b.shop.example"));
        assertFalse(NameRule.permits("shop.example", "evilshop.example"));
        assertEquals("neither", NameRule.why("shop.example", "evilshop.example"));
    }
}
