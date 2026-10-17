package com.jk.explore.security.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/** The store's order endpoints. Every request goes through the gate before anything happens. */
public class Shop {

    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Gate gate;

    public Shop(Gate gate) {
        this.gate = gate;
    }

    public Shop add(Order order) {
        orders.put(order.id(), order);
        return this;
    }

    public Response handle(String token, Action action, String orderId) {
        Order order = orders.get(orderId);
        return gate.check(token, action, order);
    }
}
