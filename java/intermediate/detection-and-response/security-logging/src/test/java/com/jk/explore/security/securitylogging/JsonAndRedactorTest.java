package com.jk.explore.security.securitylogging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonAndRedactorTest {

    @Test
    void anyTextRoundTripsAndNeverBreaksTheLine() {
        String nasty = "a\"b\\c\nd\re\tf\u0001g h, \"x\":\"y\" ü";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("actor", nasty);
        fields.put("outcome", "failure");
        String line = Json.write(fields);
        assertFalse(line.contains("\n") || line.contains("\r") || line.contains(" "));
        assertEquals(fields, Json.parse(line));
    }

    @Test
    void parsingRefusesWhatTheWriterNeverWrites() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":\"b\"}x"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":\"b\",\"a\":\"c\"}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":\"line\nbreak\"}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":\"b\""));
        assertEquals(Map.of(), Json.parse("{}"));
    }

    @Test
    void secretsAreRemovedByFieldNameWhateverTheCase() {
        Map<String, String> in = new LinkedHashMap<>();
        in.put("Password", "hunter2");
        in.put("SESSION", "s-1");
        in.put("authorization", "Basic abc");
        in.put("amount", "500");
        Map<String, String> out = Redactor.details(in);
        assertEquals(Redactor.REMOVED, out.get("Password"));
        assertEquals(Redactor.REMOVED, out.get("SESSION"));
        assertEquals(Redactor.REMOVED, out.get("authorization"));
        assertEquals("500", out.get("amount"));
    }

    @Test
    void cardNumbersKeepOnlyTheirLastFourDigitsInAnyGrouping() {
        assertEquals("************1111", Redactor.text("4111111111111111"));
        assertEquals("card ************4444 ok", Redactor.text("card 5555 5555 5555 4444 ok"));
        assertEquals("***********0005", Redactor.text("3782-822463-10005"));
        assertEquals("order 123456789012 of 500", Redactor.text("order 123456789012 of 500"));
    }

    @Test
    void tokensAreRemovedByShape() {
        assertEquals("Bearer " + Redactor.REMOVED, Redactor.text("Bearer abc.def-ghi"));
        assertTrue(Redactor.text("token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2ln end").contains(Redactor.REMOVED));
        assertFalse(Redactor.text("token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2ln end").contains("eyJ"));
    }
}
