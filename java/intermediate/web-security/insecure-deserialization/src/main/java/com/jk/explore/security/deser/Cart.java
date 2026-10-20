package com.jk.explore.security.deser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** The shopping cart as the first version of the shop kept it: a Java object, serialized into the cookie, <em>with the price of each line</em>. */
public final class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    /** One line: what, how many, and the price per unit in cents. */
    public record Line(String sku, int quantity, long priceCents) implements Serializable {
    }

    private final ArrayList<Line> lines = new ArrayList<>();

    public Cart add(String sku, int quantity, long priceCents) {
        lines.add(new Line(sku, quantity, priceCents));
        return this;
    }

    public List<Line> lines() {
        return List.copyOf(lines);
    }

    public long totalCents() {
        return lines.stream().mapToLong(l -> l.quantity() * l.priceCents()).sum();
    }
}
