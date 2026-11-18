package com.jk.explore.security.securitylogging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The worked-out values the demo prints are the ones the classes really produce. */
class DemoSectionsTest {

    @Test
    void fieldShowsAValueExactlyAsWrittenIncludingEscapedQuotes() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("actor", "mallory\",\"outcome\":\"success");
        fields.put("outcome", "failure");
        String line = Json.write(fields);
        assertEquals("\"actor\":\"mallory\\\",\\\"outcome\\\":\\\"success\"", SecurityLoggingDemo.field(line, "actor"));
        assertEquals("\"outcome\":\"failure\"", SecurityLoggingDemo.field(line, "outcome"));
    }

    @Test
    void aCardKeepsItsLastFourDigitsAfterTwelveStars() {
        assertEquals("*".repeat(12) + "1111", Redactor.text(BankDay.CARD));
        assertEquals("paid with " + "*".repeat(12) + "1111", Redactor.text("paid with 4111-1111-1111-1111"));
    }

    @Test
    void eachPrevIsTheShortHashOfTheLineBefore() throws Exception {
        List<String> log = SecurityLoggingDemo.securityLog();
        for (int i = 1; i < log.size(); i++) {
            assertEquals(Hashes.sha256(log.get(i - 1)), Json.parse(log.get(i)).get("prev"));
        }
        assertEquals(SecurityLoggingDemo.SHORT_HASH, SecurityLoggingDemo.shortHash(SecurityLog.GENESIS).length());
    }
}
