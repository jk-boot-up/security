package com.jk.explore.security.accesscontrolspring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * The orders, held in memory. Two ways to look one up: by id alone, which answers for anyone's order, and by id and owner, which
 * puts the ownership check into the query itself, the way a {@code findByIdAndOwner} method of a Spring Data repository would.
 */
@Repository
public class Orders {

    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final List<String> refunds = new ArrayList<>();

    public Orders() {
        Catalogue.orders().forEach(o -> orders.put(o.getId(), o));
    }

    /** Anyone's order with this id: right for staff, wrong for a customer's request. */
    Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    /** The order with this id only if it belongs to this owner; someone else's order is as absent as a missing one. */
    Optional<Order> findByIdAndOwner(String id, String owner) {
        return findById(id).filter(o -> o.getOwner().equals(owner));
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
