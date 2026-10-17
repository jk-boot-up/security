package com.jk.explore.security.authspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class FilterChainTest {

    @Test
    void anOpenChainPermitsEverythingToEveryone() {
        try (Shop shop = new Shop(OpenConfig.class)) {
            assertEquals(200, shop.send(HttpMethod.POST, "/orders/O-1/refund", null, null).status());
        }
    }

    @Test
    void noCredentialsOrAWrongPasswordIsA401() {
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            assertEquals(401, shop.send(HttpMethod.GET, "/orders/O-2", null, null).status());
            assertEquals(401, shop.send(HttpMethod.GET, "/orders/O-2", "ben", "guess").status());
            assertEquals(401, shop.send(HttpMethod.GET, "/orders/O-2", "nobody", "x").status());
            assertEquals(200, shop.as("ben", HttpMethod.GET, "/orders/O-2").status());
        }
    }

    @Test
    void loginOnlyLetsACustomerRefund() {
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            assertEquals(200, shop.as("ben", HttpMethod.POST, "/orders/O-1/refund").status());
            assertEquals(200, shop.as("ben", HttpMethod.GET, "/orders/O-1").status());
        }
    }

    @Test
    void thePolicyAnswers401ThenAppliesTheRules() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            assertEquals(401, shop.send(HttpMethod.POST, "/orders/O-1/refund", null, null).status());
            assertEquals(403, shop.as("ben", HttpMethod.POST, "/orders/O-1/refund").status());
            assertEquals(403, shop.as("ben", HttpMethod.GET, "/orders/O-1").status());
            assertEquals(200, shop.as("ben", HttpMethod.GET, "/orders/O-2").status());
            assertEquals(200, shop.as("sam", HttpMethod.GET, "/orders/O-1").status());
            assertEquals(200, shop.as("sam", HttpMethod.POST, "/orders/O-1/refund").status());
            assertEquals(403, shop.as("ana", HttpMethod.POST, "/orders/O-2/cancel").status());
            assertEquals(200, shop.as("alex", HttpMethod.POST, "/orders/O-2/cancel").status());
        }
    }

    @Test
    void supportMayNotCancelButAnAdminMay() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            assertEquals(403, shop.as("sam", HttpMethod.POST, "/orders/O-1/cancel").status());
            assertEquals(200, shop.as("alex", HttpMethod.POST, "/orders/O-1/cancel").status());
        }
    }

    @Test
    void aRuleWrittenOnTheMethodDecidesWhatTheBodyIs() {
        try (Shop shop = new Shop(PolicyConfig.class)) {
            assertEquals("refunded O-1", shop.as("sam", HttpMethod.POST, "/orders/O-1/refund").body());
            assertTrue(shop.as("ben", HttpMethod.POST, "/orders/O-1/refund").body().isEmpty());
        }
    }
}
