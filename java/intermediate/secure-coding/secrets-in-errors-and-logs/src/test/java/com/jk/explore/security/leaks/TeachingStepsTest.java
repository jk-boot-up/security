package com.jk.explore.security.leaks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The teaching steps the demo prints agree with the real classes. */
class TeachingStepsTest {

    @Test
    void theLuhnContributionsAddUpToTheCheckTheRedactorUses() {
        assertArrayEquals(new int[] {8, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1}, Luhn.contributions("4111111111111111"));
        assertEquals(30, Luhn.sum("4111111111111111"));
        assertEquals(31, Luhn.sum("4111111111111112"));
        assertArrayEquals(new int[] {1, 9}, Luhn.contributions("59"), "the last digit stays; the 5 is doubled to 10, and 10 - 9 = 1");
        assertTrue(Luhn.valid("5500000000000004"));
    }

    @Test
    void thePageReaderFindsWhatTheNaivePageGivesAway() {
        Checkout.Response r = Checkout.naive(new NaiveLogger(new LogSink())).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        Map<String, String> found = PageReader.read(r.body());
        assertEquals("db.internal.shop.example", found.get("host"));
        assertEquals("5432", found.get("port"));
        assertEquals("app", found.get("user"));
        assertEquals("s3cr3t-db-password", found.get("password"));
        assertEquals("Database.saveOrder, Checkout.pay", found.get("code"));
    }

    @Test
    void thePageReaderFindsNothingInTheFixedPage() {
        Checkout.Response r = Checkout.fixed(new SafeLogger(new LogSink())).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        assertTrue(PageReader.read(r.body()).isEmpty());
    }

    @Test
    void canariesAreFoundOnlyWhereTheyLeaked() {
        LogSink naive = new LogSink();
        Checkout.naive(new NaiveLogger(naive)).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        LogSink fixed = new LogSink();
        Checkout.fixed(new SafeLogger(fixed)).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        for (String name : LeaksDemo.CANARIES.names()) {
            assertTrue(LeaksDemo.CANARIES.foundIn(name, String.join("\n", naive.lines())), name);
        }
        assertFalse(LeaksDemo.CANARIES.anyFoundIn(String.join("\n", fixed.lines())));
    }

    @Test
    void eachRuleIsNamedWhenItMasksSomething() {
        Redactor r = new Redactor();
        assertEquals(List.of("card, Luhn passes"), r.rulesThatMatch("pay 4111 1111 1111 1111"));
        assertEquals(List.of("key name"), r.rulesThatMatch("password=hunter2"));
        assertEquals(List.of("JWT shape"), r.rulesThatMatch("x " + LeaksDemo.TOKEN.reveal()));
        assertEquals(List.of(), r.rulesThatMatch("order 4111111111111112 shipped"));
    }

    @Test
    void wrapKeepsEveryCharacterAndRespectsTheWidth() {
        String text = "java.sql.SQLException: Connection to " + Database.URL + " refused";
        List<String> lines = LeaksDemo.wrap(text, 60);
        assertTrue(lines.stream().allMatch(l -> l.length() <= 60));
        assertEquals(text.replace(" ", ""), String.join("", lines).replace(" ", ""));
    }
}
