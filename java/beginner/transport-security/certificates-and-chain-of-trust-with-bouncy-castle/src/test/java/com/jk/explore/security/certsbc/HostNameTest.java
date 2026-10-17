package com.jk.explore.security.certsbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HostNameTest {

    @Test
    void exactNamesMatchIgnoringCase() {
        Pki pki = new Pki();
        assertTrue(HostName.matches(pki.server.certificate(), "shop.example"));
        assertTrue(HostName.matches(pki.server.certificate(), "SHOP.Example"));
        assertFalse(HostName.matches(pki.server.certificate(), "shop.example.evil"));
        assertFalse(HostName.matches(pki.server.certificate(), "evil.example"));
    }

    @Test
    void certificateWithoutAlternativeNamesMatchesNothing() {
        Pki pki = new Pki();
        assertFalse(HostName.matches(pki.intermediate.certificate(), "Example Issuing CA"));
    }

    @Test
    void wildcardStandsForOneLabelOnly() {
        assertTrue(HostName.matchesPattern("*.example", "shop.example"));
        assertFalse(HostName.matchesPattern("*.example", "a.b.example"));
        assertFalse(HostName.matchesPattern("*.example", "example"));
        assertFalse(HostName.matchesPattern("*.example", ".example"));
        assertFalse(HostName.matchesPattern("shop.*", "shop.example"));
    }
}
