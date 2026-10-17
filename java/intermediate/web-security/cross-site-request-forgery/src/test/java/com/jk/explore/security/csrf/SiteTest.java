package com.jk.explore.security.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SiteTest {

    @Test
    void originIsSchemeAndHost() {
        assertEquals("https://bank.example", Site.origin("https://BANK.example/transfer?x=1"));
    }

    @Test
    void subdomainsAreTheSameSiteButNotTheSameOrigin() {
        assertTrue(Site.sameSite("https://reviews.bank.example", "https://bank.example/transfer"));
        assertEquals("same-site", Site.fetchSite("https://reviews.bank.example", "https://bank.example/transfer"));
        assertEquals("same-origin", Site.fetchSite("https://bank.example", "https://bank.example/transfer"));
    }

    @Test
    void anotherDomainOrSchemeIsCrossSite() {
        assertFalse(Site.sameSite("https://evil.example", "https://bank.example/transfer"));
        assertFalse(Site.sameSite("http://bank.example", "https://bank.example/transfer"));
        assertEquals("cross-site", Site.fetchSite("https://evil.example", "https://bank.example/transfer"));
    }

    @Test
    void anAddressTheUserTypedHasNoInitiator() {
        assertEquals("none", Site.fetchSite(null, "https://bank.example/account"));
    }
}
