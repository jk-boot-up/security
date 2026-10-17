package com.jk.explore.security.webhooks;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The store's orders and the money a refund event pays out. With state checks on, a payment event moves an order only from new to paid,
 * and a refund only from paid to refunded; with them off, every event is acted on, which is how a replayed refund pays out twice.
 */
final class Orders {

    enum State { NEW, PAID, REFUNDED }

    private record Order(long amount, State state, long refundedCents, int refunds) {
    }

    private final Map<String, Order> orders = new HashMap<>();
    private int changes;

    Orders add(String id, long amount) {
        orders.put(id, new Order(amount, State.NEW, 0, 0));
        return this;
    }

    State state(String id) {
        return orders.get(id).state();
    }

    /** How many times any order has changed. */
    int changes() {
        return changes;
    }

    int refunds(String id) {
        return orders.get(id).refunds();
    }

    long refundedCents(String id) {
        return orders.get(id).refundedCents();
    }

    String apply(Event event, boolean checkState) {
        Order order = orders.get(event.order());
        if (order == null) {
            return "refused: no such order";
        }
        if (order.amount() != event.amount()) {
            return "refused: the amount does not match the order";
        }
        boolean payment = event.type().equals(Event.PAID);
        if (checkState) {
            State expected = payment ? State.NEW : State.PAID;
            if (order.state() != expected) {
                return "not applied: order is " + order.state().name().toLowerCase(Locale.ROOT);
            }
        }
        Order next = payment ? new Order(order.amount(), State.PAID, order.refundedCents(), order.refunds())
                : new Order(order.amount(), State.REFUNDED, order.refundedCents() + event.amount(), order.refunds() + 1);
        orders.put(event.order(), next);
        changes++;
        return "order " + event.order() + " is now " + next.state().name().toLowerCase(Locale.ROOT)
                + (payment ? "" : ", " + event.amount() + " cents paid back");
    }
}
