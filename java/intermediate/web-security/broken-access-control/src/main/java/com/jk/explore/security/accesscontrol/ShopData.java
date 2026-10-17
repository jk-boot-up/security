package com.jk.explore.security.accesscontrol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The store's users and orders, built fresh for every run so that no act changes another. */
final class ShopData {

    /** An order id that is hard to guess, shared by ben in a gift link. */
    static final String GIFT_ORDER = "c5e1f0a8-4b2d-4c19-9e7a-3d6b2f8a1c40";

    private final List<User> users = new ArrayList<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final List<String> refunds = new ArrayList<>();

    static ShopData sample() {
        ShopData data = new ShopData();
        data.users.add(new User("ana", Role.CUSTOMER, "token-ana-7Q2x"));
        data.users.add(new User("ben", Role.CUSTOMER, "token-ben-4Lp9"));
        data.users.add(new User("cara", Role.CUSTOMER, "token-cara-8Vn3"));
        data.users.add(new User("sam", Role.SUPPORT, "token-sam-2Kd6"));
        data.users.add(new User("dave", Role.ADMIN, "token-dave-5Ht1"));
        data.add(new Order("1001", "ana", "laptop", "12 Palm Street"));
        data.add(new Order("1002", "ben", "headphones", "7 Lake Road"));
        data.add(new Order("1003", "ana", "keyboard", "12 Palm Street"));
        data.add(new Order("1004", "cara", "monitor", "3 Hill Lane"));
        data.add(new Order(GIFT_ORDER, "ben", "gift card", "7 Lake Road"));
        return data;
    }

    private void add(Order order) {
        orders.put(order.id(), order);
    }

    /**
     * The user whose token this is. Every token is compared in constant time, and every user is checked, so the time taken does not
     * say how much of a guessed token was right.
     */
    Optional<User> userFor(String token) {
        if (token == null) {
            return Optional.empty();
        }
        byte[] offered = token.getBytes(StandardCharsets.UTF_8);
        User found = null;
        for (User user : users) {
            if (MessageDigest.isEqual(offered, user.token().getBytes(StandardCharsets.UTF_8))) {
                found = user;
            }
        }
        return Optional.ofNullable(found);
    }

    User user(String name) {
        return users.stream().filter(u -> u.name().equals(name)).findFirst().orElseThrow();
    }

    boolean removeUser(String name) {
        return users.removeIf(u -> u.name().equals(name));
    }

    List<User> users() {
        return List.copyOf(users);
    }

    Optional<Order> order(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    boolean cancel(String id) {
        return orders.remove(id) != null;
    }

    void refund(String id) {
        refunds.add(id);
    }

    List<String> refunds() {
        return List.copyOf(refunds);
    }
}
