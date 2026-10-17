package com.jk.explore.security.cia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The first version of the store's order system: anyone who can reach it can read anything and
 * change anything, and nothing notices. It exists to show what each of the three properties protects.
 */
public class NaiveStore {

    private final Map<String, OrderRecord> orders = new LinkedHashMap<>();

    public void save(OrderRecord order) {
        orders.put(order.id(), order);
    }

    /** Returns the whole order to whoever asks. */
    public OrderRecord read(User asker, String id) {
        return orders.get(id);
    }

    /** Changes the amount for whoever asks, and tells nobody. */
    public void setAmount(User asker, String id, int amountDollars) {
        orders.put(id, orders.get(id).withAmount(amountDollars));
    }
}
