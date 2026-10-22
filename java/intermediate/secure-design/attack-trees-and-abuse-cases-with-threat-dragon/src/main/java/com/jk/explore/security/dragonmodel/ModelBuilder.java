package com.jk.explore.security.dragonmodel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * The bank's threat model as an OWASP Threat Dragon 2.x file: a diagram of actors, processes, a store, flows and two trust boundaries, and one threat for each abuse case, attached to the
 * element or flow it hits, with a status (Open or Mitigated), a severity from the attack tree, the abuse case as its description and the defence as its mitigation.
 */
public final class ModelBuilder {

    public static final String VERSION = "2.6.2";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ModelBuilder() {
    }

    private static String id(String key) {
        return UUID.nameUUIDFromBytes(("dragon-bank-" + key).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static Map<String, Object> position(int x, int y) {
        return Map.of("x", x, "y", y);
    }

    private static Map<String, Object> size(int w, int h) {
        return Map.of("width", w, "height", h);
    }

    private static Map<String, Object> body(String label) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stroke", "#333333");
        body.put("strokeWidth", 1.5);
        body.put("strokeDasharray", null);
        return Map.of("body", body, "label", Map.of("text", label));
    }

    private static Map<String, Object> node(String key, String shape, String type, String name, int x, int y, int w, int h, int z, Map<String, Object> extra, List<Map<String, Object>> threats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("name", name);
        data.put("description", "");
        data.put("outOfScope", false);
        data.put("reasonOutOfScope", "");
        data.put("hasOpenThreats", threats.stream().anyMatch(t -> t.get("status").equals("Open")));
        data.putAll(extra);
        data.put("threats", threats);
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("position", position(x, y));
        cell.put("size", size(w, h));
        cell.put("attrs", body(name));
        cell.put("visible", true);
        cell.put("shape", shape);
        cell.put("zIndex", z);
        cell.put("id", id(key));
        cell.put("data", data);
        return cell;
    }

    private static Map<String, Object> flow(String key, String from, String to, String name, String protocol, boolean encrypted, boolean publicNetwork, int z, List<Map<String, Object>> threats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "tm.Flow");
        data.put("name", name);
        data.put("description", "");
        data.put("outOfScope", false);
        data.put("reasonOutOfScope", "");
        data.put("hasOpenThreats", threats.stream().anyMatch(t -> t.get("status").equals("Open")));
        data.put("isBidirectional", false);
        data.put("isEncrypted", encrypted);
        data.put("isPublicNetwork", publicNetwork);
        data.put("protocol", protocol);
        data.put("threats", threats);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("stroke", "#333333");
        line.put("strokeWidth", 1.5);
        line.put("strokeDasharray", null);
        line.put("targetMarker", Map.of("name", "block"));
        line.put("sourceMarker", Map.of("name", ""));
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("attrs", Map.of("line", line));
        cell.put("connector", "smooth");
        cell.put("source", Map.of("cell", id(from)));
        cell.put("target", Map.of("cell", id(to)));
        cell.put("vertices", List.of());
        cell.put("visible", true);
        cell.put("shape", "flow");
        cell.put("zIndex", z);
        cell.put("id", id(key));
        cell.put("data", data);
        return cell;
    }

    private static Map<String, Object> boundary(String key, String name, int x, int y, int w, int h, int z) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stroke", "#333333");
        body.put("strokeWidth", 2.0);
        body.put("strokeDasharray", "4 3");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "tm.Boundary");
        data.put("name", name);
        data.put("description", "");
        data.put("isTrustBoundary", true);
        data.put("hasOpenThreats", false);
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("position", position(x, y));
        cell.put("size", size(w, h));
        cell.put("attrs", Map.of("body", body, "label", Map.of("text", name)));
        cell.put("visible", true);
        cell.put("shape", "trust-boundary-box");
        cell.put("zIndex", z);
        cell.put("id", id(key));
        cell.put("data", data);
        return cell;
    }

    /** The threat of one abuse case; the number is its position in the list, from 1. */
    public static Map<String, Object> threat(Cases.Entry entry, int number, BankConfig config, String story) {
        List<String> inPlace = new ArrayList<>();
        for (Defence d : Defence.all()) {
            if (entry.answers().contains(d.configChange()) && config.has(d.configChange())) {
                inPlace.add(d.id());
            }
        }
        boolean mitigated = !inPlace.isEmpty();
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id("threat-" + entry.id()));
        t.put("threatId", id("threat-" + entry.id()));
        t.put("title", entry.id() + ": " + story);
        t.put("status", mitigated ? "Mitigated" : "Open");
        t.put("severity", entry.severity());
        t.put("type", entry.stride());
        t.put("description", "Abuse case " + entry.id() + ". " + story + ".");
        t.put("mitigation", mitigated ? "In place: " + String.join(", ", inPlace) : "");
        t.put("modelType", "STRIDE");
        t.put("new", false);
        t.put("number", number);
        t.put("score", entry.score());
        return t;
    }

    /** The whole model for a configuration of the bank, as JSON text. */
    public static String json(BankConfig config) {
        List<Cases.Entry> entries = Cases.all(BankTree.baseline());
        Map<String, List<Map<String, Object>>> byElement = new LinkedHashMap<>();
        int number = 0;
        for (Cases.Entry e : entries) {
            AbuseCase c = AbuseCase.all().stream().filter(a -> a.id().equals(e.id())).findFirst().orElseThrow();
            byElement.computeIfAbsent(e.element(), k -> new ArrayList<>()).add(threat(e, ++number, config, c.story()));
        }
        List<Map<String, Object>> t = List.of();
        List<Map<String, Object>> cells = new ArrayList<>();
        cells.add(boundary("internet", "Internet", 20, 20, 360, 520, 0));
        cells.add(boundary("network", "Bank network", 420, 20, 700, 520, 0));
        cells.add(node("ana", "actor", "tm.Actor", "Ana (customer)", 60, 60, 160, 80, 1, Map.of("providesAuthentication", false), byElement.getOrDefault("ana", t)));
        cells.add(node("carrier", "actor", "tm.Actor", "Phone carrier", 60, 400, 160, 80, 1, Map.of("providesAuthentication", false), byElement.getOrDefault("carrier", t)));
        cells.add(node("web", "process", "tm.Process", "Web app", 480, 60, 100, 100, 1, Map.of("privilegeLevel", "", "isWebApplication", true, "handlesCardPayment", false, "handlesGoodsOrServices", false), byElement.getOrDefault("web", t)));
        cells.add(node("transfer", "process", "tm.Process", "Transfer API", 760, 60, 100, 100, 1, Map.of("privilegeLevel", "", "isWebApplication", false, "handlesCardPayment", false, "handlesGoodsOrServices", false), byElement.getOrDefault("transfer", t)));
        cells.add(node("approval", "process", "tm.Process", "Approval console", 760, 340, 100, 100, 1, Map.of("privilegeLevel", "operations", "isWebApplication", true, "handlesCardPayment", false, "handlesGoodsOrServices", false), byElement.getOrDefault("approval", t)));
        cells.add(node("accounts", "store", "tm.Store", "Accounts database", 960, 80, 160, 80, 1, Map.of("isALog", false, "isEncrypted", true, "isSigned", false, "storesCredentials", false, "storesInventory", false), byElement.getOrDefault("accounts", t)));
        cells.add(flow("signin", "ana", "web", "Sign in with password and one-time code", "HTTPS", true, true, 2, byElement.getOrDefault("signin", t)));
        cells.add(flow("request", "web", "transfer", "Transfer request", "HTTPS", true, false, 2, byElement.getOrDefault("request", t)));
        cells.add(flow("balance", "transfer", "accounts", "Balance update", "TLS", true, false, 2, byElement.getOrDefault("balance", t)));
        cells.add(flow("approve", "transfer", "approval", "Approval request", "HTTPS", true, false, 2, byElement.getOrDefault("approve", t)));
        cells.add(flow("sms", "carrier", "ana", "Text-message code", "SMS", false, true, 2, byElement.getOrDefault("sms", t)));
        Map<String, Object> diagram = new LinkedHashMap<>();
        diagram.put("id", 0);
        diagram.put("title", "Move money out of Ana's account");
        diagram.put("diagramType", "STRIDE");
        diagram.put("placeholder", "The bank's flows from sign-in to the accounts database");
        diagram.put("thumbnail", "./public/content/images/thumbnail.stride.jpg");
        diagram.put("version", VERSION);
        diagram.put("cells", cells);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("title", "Online bank: abuse cases from the attack tree");
        summary.put("owner", "payments team");
        summary.put("description", "Each threat is an abuse case of the attack tree for moving money out of Ana's account; the severity comes from the tree's cost and chance.");
        summary.put("id", 0);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("contributors", List.of(Map.of("name", "security team")));
        detail.put("diagrams", List.of(diagram));
        detail.put("diagramTop", 1);
        detail.put("reviewer", "security team");
        detail.put("threatTop", number);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("version", VERSION);
        model.put("summary", summary);
        model.put("detail", detail);
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(model);
    }
}
