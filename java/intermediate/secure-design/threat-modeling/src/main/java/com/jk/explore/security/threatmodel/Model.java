package com.jk.explore.security.threatmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A data-flow model of a system: elements in zones, the flows between them, and the trust boundaries between zones. Zones have a
 * trust level, so the model can tell when a flow goes from a less trusted zone into a more trusted one.
 */
final class Model {

    private final Map<String, Element> elements;
    private final List<Flow> flows;
    private final List<Boundary> boundaries;
    private final Map<String, Integer> zoneTrust;

    Model(List<Element> elements, List<Flow> flows, List<Boundary> boundaries, Map<String, Integer> zoneTrust) {
        Map<String, Element> byId = new LinkedHashMap<>();
        for (Element e : elements) {
            byId.put(e.id(), e);
        }
        this.elements = byId;
        this.flows = List.copyOf(flows);
        this.boundaries = List.copyOf(boundaries);
        this.zoneTrust = Map.copyOf(zoneTrust);
    }

    List<Element> elements() {
        return List.copyOf(elements.values());
    }

    List<Flow> flows() {
        return flows;
    }

    List<Boundary> boundaries() {
        return boundaries;
    }

    Element element(String id) {
        Element e = elements.get(id);
        if (e == null) {
            throw new IllegalArgumentException("no element " + id);
        }
        return e;
    }

    boolean has(String id) {
        return elements.containsKey(id);
    }

    int trust(String zone) {
        Integer t = zoneTrust.get(zone);
        if (t == null) {
            throw new IllegalArgumentException("no zone " + zone);
        }
        return t;
    }

    String zoneOf(String elementId) {
        return element(elementId).zone();
    }

    boolean crosses(Flow f) {
        return !zoneOf(f.from()).equals(zoneOf(f.to()));
    }

    Optional<Boundary> boundaryOf(Flow f) {
        String a = zoneOf(f.from());
        String b = zoneOf(f.to());
        return boundaries.stream().filter(x -> x.separates(a, b)).findFirst();
    }

    /** True when the flow goes from a less trusted zone into a more trusted one. */
    boolean raisesTrust(Flow f) {
        return trust(zoneOf(f.to())) > trust(zoneOf(f.from()));
    }

    List<Flow> into(String elementId) {
        return flows.stream().filter(f -> f.to().equals(elementId)).toList();
    }

    List<Flow> outOf(String elementId) {
        return flows.stream().filter(f -> f.from().equals(elementId)).toList();
    }

    /** What makes the model incomplete or wrong as a picture, before any threat is looked for. */
    List<String> problems() {
        List<String> problems = new ArrayList<>();
        for (Flow f : flows) {
            if (!has(f.from()) || !has(f.to())) {
                problems.add(f.id() + " names an element that is not in the model");
                continue;
            }
            if (crosses(f) && boundaryOf(f).isEmpty()) {
                problems.add(f.id() + " goes from " + zoneOf(f.from()) + " to " + zoneOf(f.to())
                        + " with no single boundary between them: it skips a boundary, or one is missing from the picture");
            }
        }
        for (Element e : elements.values()) {
            if (e.kind() == Kind.DATA_STORE && (e.owner() == null || e.owner().isBlank())) {
                problems.add(e.id() + " " + e.name() + " has no owner");
            }
            if (into(e.id()).isEmpty() && outOf(e.id()).isEmpty()) {
                problems.add(e.id() + " " + e.name() + " is not connected to anything");
            }
        }
        return problems;
    }

    Model with(Element extra) {
        List<Element> list = new ArrayList<>(elements.values());
        list.add(extra);
        return new Model(list, flows, boundaries, zoneTrust);
    }

    Model with(Flow extra) {
        List<Flow> list = new ArrayList<>(flows);
        list.add(extra);
        return new Model(elements.values().stream().toList(), list, boundaries, zoneTrust);
    }
}
