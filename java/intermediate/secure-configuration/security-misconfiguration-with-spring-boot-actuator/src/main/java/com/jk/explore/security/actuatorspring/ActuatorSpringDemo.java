package com.jk.explore.security.actuatorspring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the store story in five numbered acts against real Spring Boot servers on the loopback interface. Each act prints its
 * input (the settings, the request) and its result (the reply, the score) in separate, labelled sections. Ports, paths and disk
 * sizes are never printed, so the output is the same on every run.
 */
public class ActuatorSpringDemo {

    static final String PAYMENT_KEY = "pay-live-4f9a1c77-demo-only";
    static final String DB_PASSWORD = "pg-demo-passw0rd";
    static final String LOGGER = "/actuator/loggers/ROOT";

    private static final Wire WIRE = new Wire();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
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

    /** The settings lines of one profile's properties file, comments left out. */
    private static void settings(StringBuilder out, String file) throws IOException {
        try (InputStream in = ActuatorSpringDemo.class.getResourceAsStream("/" + file)) {
            for (String s : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!s.isBlank() && !s.startsWith("#")) {
                    line(out, "    " + s.strip());
                }
            }
        }
    }

    /** The first "value" in a reply of the env endpoint: the property's value, or ****** when it is masked. */
    static String value(String json) {
        Matcher m = Pattern.compile("\"value\":\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "(none)";
    }

    /** The names of the endpoints a list of endpoints offers, without the list's own link. */
    static TreeSet<String> endpoints(String json) {
        TreeSet<String> names = new TreeSet<>();
        Matcher m = Pattern.compile("\"([A-Za-z-]+)\":\\{\"href\"").matcher(json);
        while (m.find()) {
            if (!m.group(1).equals("self") && !m.group(1).contains("-")) {   // "health-path" and the like are templates of one endpoint
                names.add(m.group(1));
            }
        }
        return names;
    }

    /** The names of the parts a health reply reports on, or an empty set when it shows a status only. */
    static TreeSet<String> components(String json) {
        TreeSet<String> names = new TreeSet<>();
        int start = json.indexOf("\"components\":{");
        if (start < 0) {
            return names;
        }
        int depth = 0;
        for (int i = start + "\"components\":".length(); i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                break;
            } else if (c == '"' && depth == 1) {
                int end = json.indexOf('"', i + 1);
                names.add(json.substring(i + 1, end));
                i = json.indexOf('{', end);
                depth++;
            }
        }
        return names;
    }

    /** The level set on a logger, from a reply of the loggers endpoint. */
    static String level(String json) {
        Matcher m = Pattern.compile("\"configuredLevel\":\"([A-Z]+)\"").matcher(json);
        return m.find() ? m.group(1) : "(none)";
    }

    private static String unlisted(String mappings) {
        Matcher m = Pattern.compile("/internal/[^\"\\]]*").matcher(mappings);
        return m.find() ? m.group() : "(none)";
    }

    private static void checks(StringBuilder out, String who, List<ActuatorChecker.Finding> findings) {
        line(out, "  THE " + findings.size() + " CHECKS AGAINST THE " + who + " STORE (verdict, check, seen):");
        for (ActuatorChecker.Finding f : findings) {
            line(out, String.format("    %-4s  %-18s  %s", f.pass() ? "pass" : "FAIL", f.name(), f.evidence()));
        }
        line(out, "  THE SCORE:");
    }

    private static void act1Shipped(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        line(out, "ONE. A tutorial's settings: expose everything.");
        line(out, "  THE SETTINGS (application-shipped.properties):");
        settings(out, "application-shipped.properties");
        line(out, "  THE PORTS:");
        line(out, "    customers and operations share one port: " + store.sharesPort());
        line(out, "  THE REQUESTS (a stranger, no login):");
        line(out, "    GET /actuator");
        line(out, "    GET /actuator/env/shop.payment.api-key");
        line(out, "    GET /actuator/env/shop.db.password");
        line(out, "    GET /actuator/mappings");
        line(out, "    GET /actuator/health");
        line(out, "    GET /actuator/heapdump");
        line(out, "    POST /actuator/loggers/com.jk  {\"configuredLevel\":\"DEBUG\"}");
        Wire.Reply listing = WIRE.get(m + "/actuator", null);
        TreeSet<String> offered = endpoints(listing.body());
        Wire.Reply key = WIRE.get(m + "/actuator/env/shop.payment.api-key", null);
        Wire.Reply db = WIRE.get(m + "/actuator/env/shop.db.password", null);
        Wire.Reply mappings = WIRE.get(m + "/actuator/mappings", null);
        Wire.Reply health = WIRE.get(m + "/actuator/health", null);
        int dump = WIRE.status(m + "/actuator/heapdump", null);
        boolean inDump = WIRE.downloadContains(m + "/actuator/heapdump", null, PAYMENT_KEY);
        String ours = m + "/actuator/loggers/com.jk";   // the store's own code, which logs nothing, so the demo's output stays clean
        int changed = WIRE.post(ours, null, "{\"configuredLevel\":\"DEBUG\"}").status();
        String level = level(WIRE.get(ours, null).body());
        WIRE.post(ours, null, "{\"configuredLevel\":null}");   // put it back: every store in this demo shares one logging system
        line(out, "  THE REPLIES (the status, and what the page shows):");
        line(out, "    " + listing.status() + "  " + offered.size() + " endpoints, among them env, heapdump, shutdown");
        line(out, "    " + key.status() + "  \"value\":\"" + value(key.body()) + "\"  <- THE KEY");
        line(out, "    " + db.status() + "  \"value\":\"" + value(db.body()) + "\"");
        line(out, "    " + mappings.status() + "  " + unlisted(mappings.body()) + ", a route no page links to");
        line(out, "    " + health.status() + "  components:");
        line(out, "         " + String.join(", ", components(health.body())));
        line(out, "    " + dump + "  the payment key is in the dump: " + inDump + "  <- ALL MEMORY");
        line(out, "    " + changed + "  the level is now " + level);
        List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
        checks(out, "SHIPPED", findings);
        line(out, "    " + ActuatorChecker.score(findings) + " of " + findings.size() + "  <- EVERY EXPOSED ENDPOINT IS A FINDING");
        line(out, "  THE LAST REQUEST, AND ITS REPLY:");
        line(out, "    POST /actuator/shutdown  {}");
        line(out, "    -> " + WIRE.post(m + "/actuator/shutdown", null, "{}").status() + ", and the store stops answering: " + stopsAnswering(store));
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
        line(out, "TWO. What Spring Boot already does, and what it cannot do.");
        try (Running store = Running.start("default")) {
            String m = store.managementUrl();
            line(out, "  THE SETTINGS: none for the actuator");
            line(out, "  THE REPLIES:");
            line(out, "    GET /actuator         -> offers " + String.join(", ", endpoints(WIRE.get(m + "/actuator", null).body())));
            line(out, "    GET /actuator/env     -> " + WIRE.status(m + "/actuator/env", null) + ", not exposed");
            line(out, "    GET /actuator/health  ->");
            line(out, "      " + WIRE.get(m + "/actuator/health", null).body());
        }
        try (Running store = Running.start("masked")) {
            String m = store.managementUrl();
            line(out, "  THE SETTINGS (application-masked.properties):");
            settings(out, "application-masked.properties");
            line(out, "  THE REPLIES:");
            Wire.Reply key = WIRE.get(m + "/actuator/env/shop.payment.api-key", null);
            line(out, "    GET .../env/shop.payment.api-key  -> \"value\":\"" + value(key.body()) + "\"");
            line(out, "    GET .../mappings -> " + unlisted(WIRE.get(m + "/actuator/mappings", null).body()) + "  <- A MAP");
            line(out, "    GET .../heapdump -> " + WIRE.status(m + "/actuator/heapdump", null));
            line(out, "    POST .../shutdown -> " + WIRE.post(m + "/actuator/shutdown", null, "{}").status());
        }
        String more = "management.endpoint.heapdump.access=unrestricted";
        try (Running store = Running.start("masked", more)) {
            String m = store.managementUrl();
            line(out, "  THE SETTINGS (the same, and one more line):");
            line(out, "    " + more);
            line(out, "  THE REPLIES:");
            line(out, "    GET .../env/shop.payment.api-key  -> \"value\":\""
                    + value(WIRE.get(m + "/actuator/env/shop.payment.api-key", null).body()) + "\"");
            line(out, "    GET .../heapdump -> " + WIRE.status(m + "/actuator/heapdump", null) + ", the key is in it: "
                    + WIRE.downloadContains(m + "/actuator/heapdump", null, PAYMENT_KEY));
            line(out, "  masking hides values, not memory");
        }
    }

    private static void act3Separated(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        String c = store.customerUrl();
        line(out, "THREE. Hardened: another port, a short list, and rules.");
        line(out, "  THE SETTINGS (application-hardened.properties):");
        settings(out, "application-hardened.properties");
        line(out, "  THE PORTS:");
        line(out, "    customers and operations share one port: " + store.sharesPort());
        line(out, "  THE RULES (StoreSecurity, first match wins):");
        line(out, "    health: everyone; other exposed endpoints: role OPS");
        line(out, "    /products, /orders/**: everyone; anything else: denied");
        line(out, "  THE REQUESTS AND REPLIES (port, login, request -> status):");
        line(out, "    customers  -     GET /products         -> " + WIRE.status(c + "/products", null));
        line(out, "    customers  -     GET /actuator/health  -> " + WIRE.status(c + "/actuator/health", null));
        line(out, "    customers  ops   GET /actuator/health  -> " + WIRE.status(c + "/actuator/health", "ops"));
        line(out, "    operations -     GET /actuator/health  -> " + WIRE.status(m + "/actuator/health", null));
        line(out, "      " + WIRE.get(m + "/actuator/health", null).body());
        for (String endpoint : List.of("env", "heapdump", "mappings", "shutdown")) {
            line(out, String.format("    operations ops   GET /actuator/%-9s -> %d", endpoint, WIRE.status(m + "/actuator/" + endpoint, "ops")));
        }
        line(out, "  not on the list, so no rule allows them: denied for everyone");
        List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
        checks(out, "HARDENED", findings);
        line(out, "    " + ActuatorChecker.score(findings) + " of " + findings.size());
    }

    private static void act4Roles(StringBuilder out, Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        line(out, "FOUR. Who may read, and who may change.");
        line(out, "  THE REQUEST: GET " + LOGGER);
        line(out, "  THE REPLIES (by login):");
        line(out, "    no login      " + WIRE.status(m + LOGGER, null));
        line(out, "    customer ana  " + WIRE.status(m + LOGGER, "ana"));
        Wire.Reply ops = WIRE.get(m + LOGGER, "ops");
        line(out, "    operator ops  " + ops.status());
        line(out, "      " + ops.body());
        line(out, "  THE REQUEST: GET /actuator/health");
        line(out, "  THE REPLIES (by login):");
        line(out, "    customer ana  components: " + names(components(WIRE.get(m + "/actuator/health", "ana").body())));
        line(out, "    operator ops  components:");
        line(out, "      " + names(components(WIRE.get(m + "/actuator/health", "ops").body())));
        line(out, "  THE REQUEST: POST " + LOGGER + " as ops");
        line(out, "    {\"configuredLevel\":\"DEBUG\"}");
        int change = WIRE.post(m + LOGGER, "ops", "{\"configuredLevel\":\"DEBUG\"}").status();
        line(out, "  THE REPLY:");
        line(out, "    " + change + ", the level is still " + level(WIRE.get(m + LOGGER, "ops").body())
                + "  (access is read-only)");
    }

    private static String names(TreeSet<String> names) {
        return names.isEmpty() ? "(none, status only)" : String.join(", ", names);
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill: one line of convenience, and a check every time.");
        String added = "management.endpoints.web.exposure.include=*";
        try (Running store = Running.start("hardened", added)) {
            line(out, "  THE CHANGE (one line added to debug something):");
            line(out, "    " + added);
            List<ActuatorChecker.Finding> findings = new ActuatorChecker().check(store);
            line(out, "  THE CHECKER:");
            line(out, "    " + ActuatorChecker.score(findings) + " of " + findings.size());
            for (ActuatorChecker.Finding f : findings) {
                if (!f.pass()) {
                    line(out, "    FAIL  " + f.name() + "  " + f.evidence());
                }
            }
            line(out, "  THE LAYER THAT HELD:");
            line(out, "    GET /actuator/env, no login -> " + WIRE.status(store.managementUrl() + "/actuator/env", null));
        }
        line(out, "  THE BILL:");
        line(out, "    port, list, rules, access: four settings, four layers");
        line(out, "    a dependency can add endpoints; a property can expose them");
        line(out, "    so the checks run in CI, on every change");
        line(out, "    passing means these 8 checks pass, not that the store is safe");
    }
}
