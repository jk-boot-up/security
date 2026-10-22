package com.jk.explore.security.pytmmodel;

import java.util.List;

/** The online bank of the plain threat-modeling project, as plain data: who talks to whom, across which boundary, and whether each flow is encrypted, authenticated and authorized. */
public final class Design {

    /** The kinds of elements pytm knows. */
    public enum Kind {
        ACTOR("Actor"), EXTERNAL("ExternalEntity"), PROCESS("Process"), SERVER("Server"), STORE("Datastore");

        final String pytm;

        Kind(String pytm) {
            this.pytm = pytm;
        }
    }

    /** One box of the diagram: a variable name for the generated code, the name shown, the kind and the boundary it sits in. */
    public record Element(String id, String name, Kind kind, String boundary) {
    }

    /** One arrow: from, to, what it carries, the protocol and port. */
    public record Flow(String id, String from, String to, String label, String data, String protocol, int port) {
    }

    private final List<Element> elements;
    private final List<Flow> flows;

    public Design(List<Element> elements, List<Flow> flows) {
        this.elements = elements;
        this.flows = flows;
    }

    public List<Element> elements() {
        return elements;
    }

    public List<Flow> flows() {
        return flows;
    }

    public List<String> boundaries() {
        return elements.stream().map(Element::boundary).distinct().toList();
    }

    public Design with(Flow flow) {
        List<Flow> all = new java.util.ArrayList<>(flows);
        all.add(flow);
        return new Design(elements, all);
    }

    public Design with(Element element) {
        List<Element> all = new java.util.ArrayList<>(elements);
        all.add(element);
        return new Design(all, flows);
    }

    /** The bank: a customer, a mobile app, an API gateway, a payments service, two stores and a card processor. */
    public static Design bank() {
        return new Design(List.of(
                new Element("customer", "Customer", Kind.ACTOR, "Device"),
                new Element("processor", "Card processor", Kind.EXTERNAL, "Partner"),
                new Element("app", "Mobile app", Kind.PROCESS, "Device"),
                new Element("gateway", "API gateway", Kind.SERVER, "DMZ"),
                new Element("payments", "Payments service", Kind.SERVER, "Internal"),
                new Element("accounts", "Accounts database", Kind.STORE, "Internal"),
                new Element("audit", "Audit log", Kind.STORE, "Internal")), List.of(
                new Flow("f1", "customer", "app", "Login and payment request", "credentials", "HTTPS", 443),
                new Flow("f2", "app", "gateway", "Payment request with token", "payment", "HTTPS", 443),
                new Flow("f3", "gateway", "payments", "Authorized payment", "payment", "HTTPS", 8443),
                new Flow("f4", "payments", "accounts", "Balance update", "balance", "TCP", 5432),
                new Flow("f5", "payments", "audit", "Payment record", "log", "TCP", 5433),
                new Flow("f6", "payments", "processor", "Card charge", "card", "HTTPS", 443),
                new Flow("f7", "processor", "payments", "Charge result", "result", "HTTPS", 443),
                new Flow("f8", "gateway", "audit", "Request record", "log", "TCP", 5433)));
    }

    /** The draft of the plain project: the app writes card numbers straight into a card vault, across a boundary. */
    public static Design draft() {
        return bank().with(new Element("vault", "Card vault", Kind.STORE, "Internal")).with(new Flow("d1", "app", "vault", "Card number", "card", "TCP", 5434));
    }

    /** A design change: refunds are sent to the card processor. */
    public static Design withRefunds() {
        return bank().with(new Flow("f9", "payments", "processor", "Refund", "refund", "HTTP", 80));
    }
}
