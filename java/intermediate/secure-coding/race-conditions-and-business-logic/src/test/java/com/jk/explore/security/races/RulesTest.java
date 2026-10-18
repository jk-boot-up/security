package com.jk.explore.security.races;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RulesTest {

    @ParameterizedTest
    @CsvSource({
        "ANA-1, BEN-2, 0, the amount must be above zero",
        "ANA-1, BEN-2, -50, the amount must be above zero",
        "ANA-1, ANA-1, 10, a transfer to the same account",
        "ANA-1, ZED-9, 10, an unknown account",
        "ANA-1, BEN-2, 1001, not enough money",
    })
    void theCarefulBankRefusesBrokenTransfers(String from, String to, long amount, String reason) {
        Bank bank = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        assertEquals("refused: " + reason, bank.transfer(new Bank.Transfer(from, to, amount)));
        assertEquals(1000, bank.balance("ANA-1"));
        assertEquals(0, bank.balance("BEN-2"));
    }

    @Test
    void aKeyReturnsTheFirstAnswerAndCannotBeReusedForAnotherTransfer() {
        Bank bank = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        Bank.Transfer rent = new Bank.Transfer("ANA-1", "BEN-2", 300);
        assertEquals("moved 300", bank.transfer("k1", rent));
        assertEquals("already done: moved 300", bank.transfer("k1", rent));
        assertTrue(bank.transfer("k1", new Bank.Transfer("ANA-1", "BEN-2", 1)).startsWith("refused"));
        assertEquals(700, bank.balance("ANA-1"));
        assertEquals("moved 300", bank.transfer("k2", rent));
        assertEquals(400, bank.balance("ANA-1"));
    }

    @Test
    void aRefusedRequestKeepsItsAnswerForItsKey() {
        Bank bank = new Bank(true).open("ANA-1", 100).open("BEN-2", 0);
        Bank.Transfer big = new Bank.Transfer("ANA-1", "BEN-2", 300);
        assertEquals("refused: not enough money", bank.transfer("k1", big));
        bank.open("ANA-1", 1000);
        assertEquals("already done: refused: not enough money", bank.transfer("k1", big));
        assertEquals(1000, bank.balance("ANA-1"));
    }

    @Test
    void anOrderMovesOnlyForwardAndIsRefundedOnce() {
        Order order = new Order(180);
        assertTrue(order.refund().startsWith("refused"));
        assertTrue(order.ship().startsWith("refused"));
        assertEquals("paid", order.pay());
        assertTrue(order.pay().startsWith("refused"));
        assertEquals("refunded", order.refund());
        assertTrue(order.refund().startsWith("refused"));
        assertTrue(order.ship().startsWith("refused"));
        assertEquals(180, order.refunded());
        assertEquals(Order.State.REFUNDED, order.state());
    }

    @Test
    void aShippedOrderCanStillBeRefundedOnce() {
        Order order = new Order(50);
        order.pay();
        order.ship();
        assertEquals("refunded", order.refund());
        assertEquals(50, order.refunded());
    }

    @Test
    void checkoutPricesFromTheCatalogueAndAllowsOneCoupon() {
        Checkout.Request cart = new Checkout.Request(
                List.of(new Checkout.Line("boots-42", 1), new Checkout.Line("socks-3", 2)), List.of("SAVE20", "SAVE20", "SPRING20"), 1);
        Checkout.Priced priced = Checkout.careful(cart);
        assertEquals(180 + 24 - 20, priced.charged());
        assertEquals(3, priced.notes().size());
        assertEquals(-59, Checkout.careless(new Checkout.Request(List.of(new Checkout.Line("boots-42", 1)),
                List.of("SAVE20", "WELCOME20", "SPRING20"), 1)).charged());
    }

    @Test
    void checkoutRefusesUnknownProductsAndOddQuantities() {
        assertEquals(-1, Checkout.careful(new Checkout.Request(List.of(new Checkout.Line("boots-99", 1)), List.of(), 0)).charged());
        assertEquals(-1, Checkout.careful(new Checkout.Request(List.of(new Checkout.Line("boots-42", 0)), List.of(), 0)).charged());
        assertEquals(-1, Checkout.careful(new Checkout.Request(List.of(new Checkout.Line("boots-42", 100)), List.of(), 0)).charged());
    }
}
