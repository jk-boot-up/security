package com.jk.explore.security.authspring;

import java.util.Map;

/** Who owns each order. Named "orders" so a security rule can ask {@code @orders.isOwner(#id, authentication.name)}. */
public class OrderRules {

    private final Map<String, String> owners = Map.of("O-1", "ana", "O-2", "ben");

    public boolean isOwner(String orderId, String username) {
        return username.equals(owners.get(orderId));
    }
}
