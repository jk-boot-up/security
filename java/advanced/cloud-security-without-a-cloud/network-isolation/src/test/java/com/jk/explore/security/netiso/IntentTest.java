package com.jk.explore.security.netiso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** The intent and the comparison, with no Docker. */
class IntentTest {

    @Test
    void aPairIsUnorderedAndAlphabetical() {
        assertEquals("app-web", Intent.pair("web", "app"));
        assertEquals("app-web", Intent.pair("app", "web"));
    }

    @Test
    void theShopWantsSixPairs() {
        assertEquals(Set.of("app-web", "app-db", "app-cache", "admin-app", "admin-db", "admin-ci"), Intent.shop().pairs());
        assertEquals(6, Service.shop().size());
    }

    @Test
    void extraAndMissingAreTheTwoDifferences() {
        Intent intent = Intent.shop();
        Set<String> measured = Set.of("app-web", "app-db", "app-cache", "admin-app", "admin-db", "cache-db");
        assertEquals(Set.of("cache-db"), intent.extra(measured));
        assertEquals(Set.of("admin-ci"), intent.missing(measured));
        assertEquals(Set.of(), intent.extra(intent.pairs()));
        assertEquals(Set.of(), intent.missing(intent.pairs()));
    }
}
