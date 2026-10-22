package com.jk.explore.security.actuatorspring;

import java.io.IOException;
import java.util.List;

/** Runs the store story in five numbered acts against real Spring Boot servers on the loopback interface. */
public class ActuatorSpringDemo {

    static final String PAYMENT_KEY = "sk_live_4f9a1c77demo";
    static final String DB_PASSWORD = "pg-demo-passw0rd";

    private static final Wire WIRE = new Wire();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses and booleans are printed; ports, paths and disk sizes never are. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running store = Running.start("shipped")) {
            act1Shipped(out, store);
        }
        act2WhatBootDoes(out);
        try (Running store = Running.start("hardened")) {
            act3Separated(out, store);
            act4Roles(out, store);
        }
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Shipped(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        line(out, "ONE. The store with the settings from a tutorial: expose everything.");
        Wire.Reply listing = WIRE.get(m + "/actuator", null);
        line(out, "  GET /actuator: " + listing.status() + ", the list offers env, heapdump, mappings and shutdown: "
                + (listing.body().contains("\"env\"") && listing.body().contains("\"heapdump\"") && listing.body().contains("\"mappings\"") && listing.body().contains("\"shutdown\"")));
        line(out, "  the customers' port and the operations port are the same port: " + store.sharesPort());
        line(out, "  GET /actuator/env/shop.payment.api-key: the page shows the payment key: "
                + WIRE.get(m + "/actuator/env/shop.payment.api-key", null).body().contains(ActuatorSpringDemo.PAYMENT_KEY) + "  <- A STRANGER READS THE LIVE KEY");
        line(out, "  GET /actuator/env/shop.db.password: the page shows the database password: "
                + WIRE.get(m + "/actuator/env/shop.db.password", null).body().contains(DB_PASSWORD));
        line(out, "  GET /actuator/mappings: lists the unlisted route /internal/refunds: "
                + WIRE.get(m + "/actuator/mappings", null).body().contains("/internal/refunds"));
        line(out, "  GET /actuator/health: shows the components behind it: " + WIRE.get(m + "/actuator/health", null).body().contains("diskSpace"));
        line(out, "  GET /actuator/heapdump: " + WIRE.status(m + "/actuator/heapdump", null) + ", and the dump contains the payment key: "
                + WIRE.downloadContains(m + "/actuator/heapdump", null, PAYMENT_KEY) + "  <- THE WHOLE MEMORY OF THE STORE");
        String logger = m + "/actuator/loggers/com.jk.explore.security.actuatorspring";
        int changed = WIRE.post(logger, null, "{\"configuredLevel\":\"DEBUG\"}").status();
        boolean debug = WIRE.get(logger, null).body().contains("\"configuredLevel\":\"DEBUG\"");
        WIRE.post(logger, null, "{\"configuredLevel\":null}");   // put it back, the logging system is shared by every store in this demo
        line(out, "  POST /actuator/loggers to DEBUG, no login: " + changed + ", the level changed: " + debug);
        List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
        line(out, "  the checker scores the store " + ActuatorChecker.score(findings) + " of " + findings.size() + "  <- EVERY EXPOSED ENDPOINT IS A FINDING");
        Wire.Reply down = WIRE.post(m + "/actuator/shutdown", null, "{}");
        line(out, "  POST /actuator/shutdown: " + down.status() + ", and the store stops answering: " + stopsAnswering(store));
    }

    private static boolean stopsAnswering(Running store) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                WIRE.status(store.customerUrl() + "/products", null);
            } catch (IOException e) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    private static void act2WhatBootDoes(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. What Spring Boot already does, and what it cannot do for you.");
        try (Running store = Running.start("default")) {
            String m = store.managementUrl();
            Wire.Reply listing = WIRE.get(m + "/actuator", null);
            line(out, "  no settings at all: the list offers health only: "
                    + (listing.body().contains("\"health\"") && !listing.body().contains("\"env\"") && !listing.body().contains("\"mappings\"")));
            line(out, "  GET /actuator/env: " + WIRE.status(m + "/actuator/env", null) + "  (not exposed)");
            line(out, "  GET /actuator/health shows components: " + WIRE.get(m + "/actuator/health", null).body().contains("components") + "  (status only)");
        }
        try (Running store = Running.start("masked")) {
            String m = store.managementUrl();
            Wire.Reply key = WIRE.get(m + "/actuator/env/shop.payment.api-key", null);
            line(out, "  include=* and nothing else: the payment key is shown as ******: " + (key.body().contains("\"value\":\"******\"") && !key.body().contains(PAYMENT_KEY)));
            line(out, "  but the property names and the route map are still open: "
                    + (key.status() == 200 && WIRE.get(m + "/actuator/mappings", null).body().contains("/internal/refunds")) + "  <- A MAP FOR THE NEXT STEP");
            line(out, "  GET /actuator/heapdump: " + WIRE.status(m + "/actuator/heapdump", null) + ", POST /actuator/shutdown: "
                    + WIRE.post(m + "/actuator/shutdown", null, "{}").status() + "  (both stay off until someone turns them on)");
        }
        try (Running store = Running.start("masked", "management.endpoint.heapdump.access=unrestricted")) {
            String m = store.managementUrl();
            line(out, "  one more line, heapdump access unrestricted: the key is masked in env, yet the dump contains it: "
                    + WIRE.downloadContains(m + "/actuator/heapdump", null, PAYMENT_KEY) + "  <- MASKING HIDES VALUES, NOT MEMORY");
        }
    }

    private static void act3Separated(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        line(out, "THREE. Hardened: another port, a short list, and rules.");
        line(out, "  the customers' port and the operations port are different ports: " + !store.sharesPort());
        line(out, "  GET /products on the customers' port: " + WIRE.status(store.customerUrl() + "/products", null));
        line(out, "  GET /actuator/health on the customers' port: " + WIRE.status(store.customerUrl() + "/actuator/health", null) + ", with the operator's login: "
                + WIRE.status(store.customerUrl() + "/actuator/health", "ops") + "  (nothing to find there)");
        Wire.Reply health = WIRE.get(m + "/actuator/health", null);
        line(out, "  GET /actuator/health on the operations port, no login: " + health.status() + ", status UP: " + health.body().contains("\"UP\"")
                + ", components shown: " + health.body().contains("components"));
        for (String endpoint : List.of("env", "heapdump", "mappings", "beans", "shutdown")) {
            line(out, "  GET /actuator/" + endpoint + " as an operator: " + WIRE.status(m + "/actuator/" + endpoint, "ops") + "  (refused by the catch-all rule)");
        }
        List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
        for (ActuatorChecker.Finding f : findings) {
            line(out, "  " + f.name() + ": " + (f.pass() ? "pass" : "FAIL") + " (" + f.evidence() + ")");
        }
        line(out, "  the checker scores the hardened store " + ActuatorChecker.score(findings) + " of " + findings.size());
    }

    private static void act4Roles(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        line(out, "FOUR. Who may read, and who may change.");
        line(out, "  GET /actuator/loggers/ROOT: no login " + WIRE.status(m + "/actuator/loggers/ROOT", null) + ", customer ana "
                + WIRE.status(m + "/actuator/loggers/ROOT", "ana") + ", operator " + WIRE.status(m + "/actuator/loggers/ROOT", "ops"));
        line(out, "  GET /actuator/health with details: customer ana sees components " + WIRE.get(m + "/actuator/health", "ana").body().contains("components")
                + ", operator sees components " + WIRE.get(m + "/actuator/health", "ops").body().contains("components"));
        int change = WIRE.post(m + "/actuator/loggers/ROOT", "ops", "{\"configuredLevel\":\"DEBUG\"}").status();
        line(out, "  the operator posts DEBUG to the loggers endpoint: " + change + ", the level is still OFF: "
                + WIRE.get(m + "/actuator/loggers/ROOT", "ops").body().contains("\"configuredLevel\":\"OFF\"") + "  (access is read-only)");
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill: one line of convenience, and a check that must run every time.");
        try (Running store = Running.start("hardened", "management.endpoints.web.exposure.include=*")) {
            List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
            line(out, "  a developer adds include=* to the hardened store to debug something: the checker scores " + ActuatorChecker.score(findings) + " of " + findings.size());
            for (ActuatorChecker.Finding f : findings) {
                if (!f.pass()) {
                    line(out, "    found: " + f.name() + " (" + f.evidence() + ")");
                }
            }
            line(out, "  the login rules still held for strangers: GET /actuator/env without a login: " + WIRE.status(store.managementUrl() + "/actuator/env", null));
        }
        line(out, "  one layer failed and another held, which is why the port, the list, the rules and the access levels are four separate settings");
        line(out, "  the same checks belong in CI on every change, because a dependency can add endpoints and a property can switch them on");
        line(out, "  a passing score means these 8 checks pass, not that the store is secure");
    }
}
