package com.jk.explore.security.threatmodel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** The online bank used throughout the demo: a mobile app, an API gateway, a payments service, two stores and a card processor. */
final class BankModel {

    static final String DEVICE = "device";
    static final String DMZ = "dmz";
    static final String INTERNAL = "internal";
    static final String PARTNER = "partner";

    private BankModel() {
    }

    static Model baseline() {
        List<Element> elements = List.of(
                new Element("E1", "customer", Kind.EXTERNAL_ENTITY, DEVICE, null, false, Set.of("passkey")),
                new Element("E2", "card processor", Kind.EXTERNAL_ENTITY, PARTNER, "processor contract", false, Set.of()),
                new Element("P1", "mobile app", Kind.PROCESS, DEVICE, "mobile team", false, Set.of()),
                new Element("P2", "API gateway", Kind.PROCESS, DMZ, "platform team", true, Set.of("token-check", "authorization", "rate-limit")),
                new Element("P3", "payments service", Kind.PROCESS, INTERNAL, "payments team", false, Set.of("authorization")),
                new Element("S1", "accounts database", Kind.DATA_STORE, INTERNAL, "payments team", false, Set.of("encrypted-at-rest")),
                new Element("S2", "audit log", Kind.DATA_STORE, INTERNAL, "security team", false, Set.of("encrypted-at-rest")));
        List<Flow> flows = List.of(
                new Flow("F1", "E1", "P1", "login and payment request", false, false, false),
                new Flow("F2", "P1", "P2", "payment request with token", true, true, true),
                new Flow("F3", "P2", "P3", "authorized payment", true, true, true),
                new Flow("F4", "P3", "S1", "balance update", true, true, true),
                new Flow("F5", "P3", "S2", "payment record", true, true, false),
                new Flow("F6", "P3", "E2", "card charge", true, true, true),
                new Flow("F7", "E2", "P3", "charge result", true, true, false),
                new Flow("F8", "P2", "S2", "request record", true, true, false));
        List<Boundary> boundaries = List.of(
                new Boundary("internet boundary", DEVICE, DMZ),
                new Boundary("gateway boundary", DMZ, INTERNAL),
                new Boundary("partner boundary", INTERNAL, PARTNER));
        Map<String, Integer> trust = Map.of(DEVICE, 0, PARTNER, 1, DMZ, 1, INTERNAL, 2);
        return new Model(elements, flows, boundaries, trust);
    }

    /** A first draft with two mistakes: the app writes card numbers straight into a store, and the store has no owner. */
    static Model draft() {
        return baseline()
                .with(new Element("S3", "card vault", Kind.DATA_STORE, INTERNAL, null, false, Set.of()))
                .with(new Flow("D1", "P1", "S3", "card number", true, false, true));
    }

    /** A design change: refunds are sent to the card processor, and the new flow is not encrypted. */
    static Model withRefunds() {
        return baseline().with(new Flow("F9", "P3", "E2", "refund", false, true, true));
    }
}
