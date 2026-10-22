package com.jk.explore.security.actuatorspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Each test starts a real store on a loopback port and talks to it over HTTP. */
class ActuatorSpringTest {

    private final Wire wire = new Wire();

    @Test
    void shippedStoreSharesOnePortAndGivesAwayItsSecrets() throws Exception {
        try (Running store = Running.start("shipped")) {
            String m = store.managementUrl();
            assertTrue(store.sharesPort());
            assertTrue(wire.get(m + "/actuator/env/shop.payment.api-key", null).body().contains(ActuatorSpringDemo.PAYMENT_KEY));
            assertTrue(wire.get(m + "/actuator/env/shop.db.password", null).body().contains(ActuatorSpringDemo.DB_PASSWORD));
            assertTrue(wire.downloadContains(m + "/actuator/heapdump", null, ActuatorSpringDemo.PAYMENT_KEY));
            assertEquals(0, ActuatorChecker.score(new ActuatorChecker().check(store)));
        }
    }

    @Test
    void shutdownEndpointStopsTheStoreForAnyone() throws Exception {
        try (Running store = Running.start("shipped")) {
            assertEquals(200, wire.post(store.managementUrl() + "/actuator/shutdown", null, "{}").status());
            boolean stopped = false;
            for (int i = 0; i < 100 && !stopped; i++) {
                try {
                    wire.status(store.customerUrl() + "/products", null);
                    Thread.sleep(50);
                } catch (java.io.IOException e) {
                    stopped = true;
                }
            }
            assertTrue(stopped);
        }
    }

    @Test
    void bareBootExposesHealthOnly() throws Exception {
        try (Running store = Running.start("default")) {
            String m = store.managementUrl();
            String listing = wire.get(m + "/actuator", null).body();
            assertTrue(listing.contains("\"health\""));
            assertFalse(listing.contains("\"env\""));
            assertEquals(404, wire.status(m + "/actuator/env", null));
            assertFalse(wire.get(m + "/actuator/health", null).body().contains("components"));
        }
    }

    @Test
    void exposeEverythingMasksValuesButNotNamesOrMemory() throws Exception {
        try (Running store = Running.start("masked")) {
            String m = store.managementUrl();
            String key = wire.get(m + "/actuator/env/shop.payment.api-key", null).body();
            assertTrue(key.contains("\"value\":\"******\""));
            assertFalse(key.contains(ActuatorSpringDemo.PAYMENT_KEY));
            assertTrue(wire.get(m + "/actuator/mappings", null).body().contains("/internal/refunds"));
            assertEquals(404, wire.status(m + "/actuator/heapdump", null));
            assertEquals(404, wire.post(m + "/actuator/shutdown", null, "{}").status());
        }
        try (Running store = Running.start("masked", "management.endpoint.heapdump.access=unrestricted")) {
            assertTrue(wire.downloadContains(store.managementUrl() + "/actuator/heapdump", null, ActuatorSpringDemo.PAYMENT_KEY));
        }
    }

    @Test
    void hardenedStoreUsesAnotherPortAndScoresEightOfEight() throws Exception {
        try (Running store = Running.start("hardened")) {
            assertFalse(store.sharesPort());
            assertEquals(200, wire.status(store.customerUrl() + "/products", null));
            assertNotEquals(200, wire.status(store.customerUrl() + "/actuator/health", null));
            assertNotEquals(200, wire.status(store.customerUrl() + "/actuator/health", "ops"));
            List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
            assertEquals(8, findings.size());
            assertEquals(8, ActuatorChecker.score(findings));
        }
    }

    @Test
    void healthDetailsAreForOperatorsOnly() throws Exception {
        try (Running store = Running.start("hardened")) {
            String h = store.managementUrl() + "/actuator/health";
            assertEquals(200, wire.status(h, null));
            assertFalse(wire.get(h, null).body().contains("components"));
            assertFalse(wire.get(h, "ana").body().contains("components"));
            assertTrue(wire.get(h, "ops").body().contains("components"));
        }
    }

    @Test
    void loggersAreReadableByOperatorsOnlyAndNeverWritable() throws Exception {
        try (Running store = Running.start("hardened")) {
            String url = store.managementUrl() + "/actuator/loggers/ROOT";
            assertEquals(401, wire.status(url, null));
            assertEquals(403, wire.status(url, "ana"));
            assertEquals(200, wire.status(url, "ops"));
            assertEquals(405, wire.post(url, "ops", "{\"configuredLevel\":\"DEBUG\"}").status());
            assertTrue(wire.get(url, "ops").body().contains("\"configuredLevel\":\"OFF\""));
        }
    }

    @Test
    void notExposedEndpointsAreRefusedEvenForOperators() throws Exception {
        try (Running store = Running.start("hardened")) {
            for (String endpoint : List.of("env", "heapdump", "mappings", "beans", "shutdown", "configprops")) {
                assertNotEquals(200, wire.status(store.managementUrl() + "/actuator/" + endpoint, "ops"), endpoint);
            }
        }
    }

    @Test
    void oneLineOfExposeEverythingCostsOneCheckAndStrangersStillGetNothing() throws Exception {
        try (Running store = Running.start("hardened", "management.endpoints.web.exposure.include=*")) {
            List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
            assertEquals(7, ActuatorChecker.score(findings));
            assertEquals(List.of("route map hidden, even from an operator"), findings.stream().filter(f -> !f.pass()).map(ActuatorChecker.Finding::name).toList());
            assertEquals(401, wire.status(store.managementUrl() + "/actuator/env", null));
        }
    }

    @Test
    void checkerChangesNothing() throws Exception {
        try (Running store = Running.start("shipped")) {
            String logger = store.managementUrl() + "/actuator/loggers/ROOT";
            String before = wire.get(logger, null).body();
            new ActuatorChecker().check(store);
            assertEquals(before, wire.get(logger, null).body());
            assertEquals(200, wire.status(store.customerUrl() + "/products", null));
        }
    }
}
