package com.jk.explore.security.races;

import java.util.Locale;

/**
 * An order as a state machine: created, then paid, then shipped, and refunded at most once, only after it was paid. Each method
 * moves the order on only from the states where that step makes sense, and refuses otherwise.
 */
final class Order {

    enum State { CREATED, PAID, SHIPPED, REFUNDED }

    private final long total;
    private State state = State.CREATED;
    private long refunded;

    Order(long total) {
        this.total = total;
    }

    State state() {
        return state;
    }

    long refunded() {
        return refunded;
    }

    String pay() {
        return move(state == State.CREATED, State.PAID, "pay");
    }

    String ship() {
        return move(state == State.PAID, State.SHIPPED, "ship");
    }

    String refund() {
        String answer = move(state == State.PAID || state == State.SHIPPED, State.REFUNDED, "refund");
        if (state == State.REFUNDED && refunded == 0) {
            refunded = total;
        }
        return answer;
    }

    private String move(boolean allowed, State to, String step) {
        if (!allowed) {
            return "refused: cannot " + step + " an order that is " + state.name().toLowerCase(Locale.ROOT);
        }
        state = to;
        return to.name().toLowerCase(Locale.ROOT);
    }

    /** The careless version: flags instead of states, so nothing stops a second refund or shipping before payment. */
    static final class Careless {
        private final long total;
        private boolean paid;
        private boolean shipped;
        private long refunded;

        Careless(long total) {
            this.total = total;
        }

        void pay() {
            paid = true;
        }

        void ship() {
            shipped = true;
        }

        void refund() {
            refunded += total;
        }

        boolean paid() {
            return paid;
        }

        boolean shipped() {
            return shipped;
        }

        long refunded() {
            return refunded;
        }
    }
}
