package com.jk.explore.security.authspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class BillTest {

    @Test
    void anEndpointWithoutARuleIsOpenToEveryLoggedInUser() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            assertEquals(200, shop.as("ben", HttpMethod.GET, "/orders/export").status());
        }
    }

    @Test
    void aRuleOnTheAddressGuardsItAnyway() {
        try (Shop shop = new Shop(LayeredConfig.class)) {
            assertEquals(403, shop.as("ben", HttpMethod.GET, "/orders/export").status());
            assertEquals(200, shop.as("alex", HttpMethod.GET, "/orders/export").status());
            assertEquals(401, shop.send(HttpMethod.GET, "/orders/export", null, null).status());
        }
    }

    @Test
    void basicAuthChecksThePasswordOnEveryRequest() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            int before = shop.encoder().checks();
            shop.as("sam", HttpMethod.GET, "/orders/O-1");
            shop.as("sam", HttpMethod.GET, "/orders/O-2");
            shop.as("sam", HttpMethod.POST, "/orders/O-1/refund");
            assertEquals(3, shop.encoder().checks() - before);
        }
    }

    @Test
    void anAnonymousRequestChecksNoPassword() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            int before = shop.encoder().checks();
            shop.send(HttpMethod.GET, "/orders/O-1", null, null);
            assertEquals(0, shop.encoder().checks() - before);
        }
    }

    @Test
    void storedPasswordsAreBCryptHashesNotThePasswords() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            String stored = shop.encoder().encode("ben-pass");
            assertTrue(stored.startsWith("$2a$04$"));
            assertFalse(stored.contains("ben-pass"));
            assertTrue(shop.encoder().matches("ben-pass", stored));
        }
    }
}
