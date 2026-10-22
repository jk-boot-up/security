package com.jk.explore.security.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/** Logback 1.6 and logstash-logback-encoder 9, writing into memory. */
class LoggingTest {

    @Test
    void aLongNumberIsACardOnlyIfItPassesTheLuhnCheck() {
        assertEquals("charge [CARD ****1111]", Masker.mask("charge 4111 1111 1111 1111"));
        assertEquals("order 4111111111111112", Masker.mask("order 4111111111111112"));
        assertTrue(Masker.luhn("4111111111111111"));
        assertFalse(Masker.luhn("4111111111111112"));
    }

    @Test
    void thePairsTheUrlsAndTheTokensAreCovered() {
        assertEquals("password=[REDACTED]&user=ana", Masker.mask("password=hunter2&user=ana"));
        assertEquals("{\"api_key\":\"[REDACTED]\"}", Masker.mask("{\"api_key\":\"k-123\"}"));
        assertEquals("jdbc:postgresql://db:5432/shop?user=app&password=[REDACTED]", Masker.mask("jdbc:postgresql://db:5432/shop?user=app&password=s3cr3t"));
        assertEquals("Authorization: Bearer [REDACTED]", Masker.mask("Authorization: Bearer abcdefgh12345"));
        assertEquals("[REDACTED]", Masker.mask(LoggingDemo.TOKEN));
    }

    @Test
    void maskingIsIdempotent() {
        String once = Masker.mask("password=hunter2 token=" + LoggingDemo.TOKEN + " 4111 1111 1111 1111 jdbc:x://h:1/db?user=u&password=p");
        assertEquals(once, Masker.mask(once));
    }

    @Test
    void aShapeNobodyListedIsNotCovered() {
        assertEquals("my pin is 4921 and the code is hunter2", Masker.mask("my pin is 4921 and the code is hunter2"));
    }

    @Test
    void aPlainLayoutKeepsEverythingAndALineBreakForgesAnEvent() {
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            LoggingDemo.careless(log.logger());
            String out = log.output();
            assertTrue(out.contains("password=s3cr3t-db-password") && out.contains("cvv=737") && out.contains(LoggingDemo.TOKEN));
            assertEquals(4, out.lines().count());
        }
    }

    @Test
    void theMaskingLayoutCoversSecretsInMessagesAndInExceptionChainsAndKeepsOneEventToOneLine() {
        try (LogSetup log = LogSetup.pattern("%level %mask%maskedEx%n")) {
            LoggingDemo.careless(log.logger());
            log.logger().error("outer", new RuntimeException("outer token=abc", new SQLException("inner password=p4ss")));
            String out = log.output();
            assertFalse(out.contains("s3cr3t") || out.contains("737") || out.contains(LoggingDemo.TOKEN) || out.contains("p4ss") || out.contains("token=abc"));
            assertEquals(4, out.lines().count());
            assertTrue(out.contains("caused by SQLException: inner password=[REDACTED]"));
        }
    }

    @Test
    void typedValuesPrintSafelyWithoutAnyMasking() {
        CheckoutRequest request = new CheckoutRequest("Ana", new Card("4111111111111111"), new Secret("737"), 1042);
        try (LogSetup log = LogSetup.pattern("%level %msg%n")) {
            log.logger().info("started: {} {}", request, new Secret(LoggingDemo.TOKEN));
            String out = log.output();
            assertTrue(out.contains("card=****1111") && out.contains("cvv=[REDACTED]"));
            assertFalse(out.contains("4111111111111111") || out.contains("737") || out.contains("eyJ"));
        }
        assertEquals("737", request.cvv().reveal());
    }

    @Test
    void jsonKeepsAnEventOnOneLineWhateverTheValueContains() {
        try (LogSetup log = LogSetup.json(null)) {
            log.logger().info("checkout started for {}", LoggingDemo.FORGED);
            assertEquals(1, log.output().lines().count());
            assertTrue(log.output().contains("\\n"));
        }
    }

    @Test
    void aRecordIsWrittenAsItsFieldsUnlessItSaysOtherwiseAndPathMasksCoverThem() {
        try (LogSetup log = LogSetup.json(null)) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")));
            log.logger().info("charge", keyValue("card", new Card("4111111111111111")));
            String[] lines = log.output().split("\n");
            assertTrue(lines[0].contains("\"number\":\"4111111111111111\""));
            assertTrue(lines[1].contains("\"card\":\"****1111\""));
        }
        try (LogSetup log = LogSetup.json(LoggingDemo.masking())) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")), keyValue("cvv", 737));
            assertFalse(log.output().contains("4111111111111111") || log.output().contains("737"));
            assertTrue(log.output().contains("\"holder\":\"Ana\""));
        }
    }

    @Test
    void theJsonMaskerCoversTheShapesOfSecretsInEveryString() {
        try (LogSetup log = LogSetup.json(LoggingDemo.masking())) {
            log.logger().error("checkout failed", LoggingDemo.databaseDown());
            assertFalse(log.output().contains("s3cr3t"));
            assertTrue(log.output().contains("password=[REDACTED]"));
        }
    }

    @Test
    void theMdcTravelsWithEveryEventAsAField() {
        try (LogSetup log = LogSetup.json(null)) {
            log.mdc("requestId", "r-77");
            log.logger().info("one");
            log.logger().info("two");
            assertEquals(2, log.output().lines().filter(l -> l.contains("\"requestId\":\"r-77\"")).count());
        }
    }
}
