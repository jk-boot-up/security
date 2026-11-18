package com.jk.explore.security.actuatorspring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests a running store from outside, as a scanner would, and scores it. A check never changes anything: the loggers check posts the level the
 * logger already has, and the shutdown check only reads the list of endpoints.
 */
final class ActuatorChecker {

    record Finding(String name, boolean pass, String evidence) {
    }

    private final Wire wire = new Wire();

    List<Finding> check(Running store) throws IOException, InterruptedException {
        String m = store.managementUrl();
        List<Finding> out = new ArrayList<>();
        out.add(refused("env hidden", "GET /actuator/env", wire.status(m + "/actuator/env", null)));
        out.add(refused("heap dump hidden", "GET /actuator/heapdump", wire.status(m + "/actuator/heapdump", null)));
        Wire.Reply listing = wire.get(m + "/actuator", "ops");
        boolean shutdown = listing.body().contains("\"shutdown\"");
        out.add(new Finding("no shutdown", !shutdown, "ops's list offers shutdown: " + shutdown));
        int post = wire.post(m + "/actuator/loggers/ROOT", "ops", "{\"configuredLevel\":\"OFF\"}").status();
        out.add(new Finding("loggers read-only", post < 200 || post >= 300, "POST .../loggers/ROOT, ops -> " + post));
        out.add(refused("route map hidden", "GET /actuator/mappings, ops", wire.status(m + "/actuator/mappings", "ops")));
        Wire.Reply health = wire.get(m + "/actuator/health", null);
        boolean details = health.body().contains("components");
        out.add(new Finding("health status only", !details, "strangers see components: " + details));
        int onCustomerPort = wire.status(store.customerUrl() + "/actuator/health", null);
        out.add(new Finding("customers' port", onCustomerPort != 200, "GET /actuator/health there -> " + onCustomerPort));
        out.add(refused("list needs a login", "GET /actuator", wire.status(m + "/actuator", null)));
        return out;
    }

    static int score(List<Finding> findings) {
        return (int) findings.stream().filter(Finding::pass).count();
    }

    private static Finding refused(String name, String request, int status) {
        return new Finding(name, status != 200, request + " -> " + status);
    }
}
