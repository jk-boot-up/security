package com.jk.explore.security.races;

import java.util.ArrayList;
import java.util.List;

/**
 * Every order in which the steps of two requests can run: each way to interleave {@code a} steps of A with {@code b} steps of B,
 * such as "AAABBB", "AABABB" or "ABABAB". Three steps each give 20 orders, four steps each give 70. The demo and the tests run all
 * of them, so a fix is checked in every order, not only in the one that was printed.
 */
final class EveryOrder {

    private EveryOrder() {
    }

    static List<String> of(int a, int b) {
        List<String> orders = new ArrayList<>();
        build("", a, b, orders);
        return orders;
    }

    private static void build(String prefix, int a, int b, List<String> orders) {
        if (a == 0 && b == 0) {
            orders.add(prefix);
            return;
        }
        if (a > 0) {
            build(prefix + "A", a - 1, b, orders);
        }
        if (b > 0) {
            build(prefix + "B", a, b - 1, orders);
        }
    }
}
