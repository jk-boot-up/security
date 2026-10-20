package com.jk.explore.security.leaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class LeaksTest {

    private static final String[] CANARIES = {"4111 1111 1111 1111", "4111111111111111", "s3cr3t-db-password", "c2lnbmF0dXJl", "cvv=737"};

    private final Redactor redactor = new Redactor();

    @Test
    void theNaiveCheckoutShowsTheCustomerTheDatabasePasswordAndTheCallChain() {
        Checkout.Response r = Checkout.naive(new NaiveLogger(new LogSink())).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        assertEquals(500, r.status());
        assertTrue(r.body().contains("password=s3cr3t-db-password"));
        assertTrue(r.body().contains("db.internal.shop.example"));
        assertTrue(r.body().contains("\tat com.jk.explore.security.leaks.Checkout.pay"));
    }

    @Test
    void theNaiveLogKeepsCardCvvTokenAndPassword() {
        LogSink sink = new LogSink();
        Checkout.naive(new NaiveLogger(sink)).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        assertTrue(sink.anyContains("4111 1111 1111 1111"));
        assertTrue(sink.anyContains("cvv=737"));
        assertTrue(sink.anyContains(LeaksDemo.TOKEN.reveal()));
        assertTrue(sink.anyContains("s3cr3t-db-password"));
    }

    @Test
    void aCustomerCanForgeALogLineInTheNaiveLogger() {
        LogSink sink = new LogSink();
        Checkout.naive(new NaiveLogger(sink)).pay(new TreeMap<>(Map.of("name", LeaksDemo.FORGED_NAME, "order", "7")), LeaksDemo.TOKEN);
        assertTrue(sink.lines().get(0).lines().anyMatch(l -> l.startsWith("INFO login succeeded user=admin")));
    }

    @Test
    void theFixedCheckoutAnswersWithAReferenceAndLogsTheDetailsWithoutSecrets() {
        LogSink sink = new LogSink();
        Checkout.Response r = Checkout.fixed(new SafeLogger(sink)).pay(LeaksDemo.ORDER, LeaksDemo.TOKEN);
        assertEquals("Something went wrong. Quote reference ref-0001 to support.", r.body());
        assertTrue(sink.lines().get(1).contains("[ref-0001]"));
        assertTrue(sink.anyContains("SQLException"));
        for (String canary : CANARIES) {
            assertFalse(sink.anyContains(canary), "log must not contain " + canary);
            assertFalse(r.body().contains(canary));
        }
    }

    @Test
    void theFixedLoggerKeepsEachEventOnOneLine() {
        LogSink sink = new LogSink();
        new SafeLogger(sink).info("a\nb\r\nc\td\u0007e f");
        assertEquals(1, sink.lines().size());
        assertEquals("INFO a\\nb\\r\\nc\\td\\u0007e\\u2028f", sink.lines().get(0));
    }

    @Test
    void longLinesAreCut() {
        LogSink sink = new LogSink();
        new SafeLogger(sink).info("x".repeat(1000));
        assertTrue(sink.lines().get(0).endsWith("...[cut]"));
        assertTrue(sink.lines().get(0).length() < 320);
    }

    @Test
    void secretsPrintAsRedacted() {
        assertEquals("[REDACTED]", new Secret("hunter2").toString());
        assertEquals("hunter2", new Secret("hunter2").reveal());
        assertEquals("a [REDACTED] b", "a " + new Secret("hunter2") + " b");
    }

    @Test
    void cardNumbersAreMaskedOnlyWhenTheyPassTheLuhnCheck() {
        assertTrue(Luhn.valid("4111111111111111"));
        assertFalse(Luhn.valid("4111111111111112"));
        assertEquals("pay [CARD ****1111] now", redactor.redact("pay 4111 1111 1111 1111 now"));
        assertEquals("pay [CARD ****1111] now", redactor.redact("pay 4111-1111-1111-1111 now"));
        assertEquals("order 4111111111111112 ok", redactor.redact("order 4111111111111112 ok"));
        assertEquals("id 12345678", redactor.redact("id 12345678"));
        assertEquals("[CARD ****0004] and [CARD ****1111]", redactor.redact("5500000000000004 and 4111111111111111"));
    }

    @Test
    void credentialsAreMaskedByKeyNameInManyShapes() {
        assertEquals("password=[REDACTED]&user=ana", redactor.redact("password=hunter2&user=ana"));
        assertEquals("{\"api_key\":\"[REDACTED]\"}", redactor.redact("{\"api_key\":\"AKIA123456\"}"));
        assertEquals("Authorization: [REDACTED]", redactor.redact("Authorization: Bearer abc123.def456.ghi789"));
        assertEquals("seen Bearer [REDACTED]", redactor.redact("seen Bearer abc123def456"));
        assertEquals("x [REDACTED-JWT] y", redactor.redact("x eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl y"));
        assertEquals("postgres://app:[REDACTED]@db/shop", redactor.redact("postgres://app:pw123@db/shop"));
        assertEquals("nothing to hide", redactor.redact("nothing to hide"));
    }

    @Test
    void redactionIsASecondLineOfDefenceNotAPromise() {
        assertEquals("the key is hunter2", redactor.redact("the key is hunter2"), "a secret with no name and no shape cannot be recognised");
    }
}
