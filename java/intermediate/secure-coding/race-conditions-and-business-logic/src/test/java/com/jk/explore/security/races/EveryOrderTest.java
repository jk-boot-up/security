package com.jk.explore.security.races;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The orders the demo counts, and the two numbers it prints: 18 of 20 orders overdraw, and only the two in-turn orders do not. */
class EveryOrderTest {

    @Test
    void threeStepsEachGiveTwentyDistinctOrdersAndFourGiveSeventy() {
        List<String> six = EveryOrder.of(3, 3);
        assertEquals(20, six.size());
        assertEquals(20, new HashSet<>(six).size());
        assertEquals("AAABBB", six.getFirst());
        assertEquals("BBBAAA", six.getLast());
        assertEquals(70, EveryOrder.of(4, 4).size());
    }

    @Test
    void checkThenActIsSafeOnlyWhenOneRequestRunsAfterTheOther() {
        List<String> safe = EveryOrder.of(3, 3).stream().filter(order -> {
            Ledger ledger = new Ledger().open("ANA-1", 100);
            Interleaving.run(order, new NaiveWithdrawal("A", ledger, "ANA-1", 80), new NaiveWithdrawal("B", ledger, "ANA-1", 80));
            return ledger.paidOut("ANA-1") == 80;
        }).toList();
        assertEquals(List.of("AAABBB", "BBBAAA"), safe);
    }

    @Test
    void checkoutShowsHowEachTotalIsMade() {
        Checkout.Request cart = new Checkout.Request(List.of(new Checkout.Line("boots-42", 1)),
                List.of("SUMMER20", "WELCOME20", "SPRING20"), 1);
        assertEquals("1 - 20 - 20 - 20", Checkout.careless(cart).working());
        assertEquals("180 - 20", Checkout.careful(cart).working());
    }
}
