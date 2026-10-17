package com.jk.explore.security.patching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void readsEveryKindOfValue() {
        Map<String, Object> object = Json.object(Json.parse(
                "{\"a\": [1, 2.5, -3], \"b\": \"x\\ny\\u0041\", \"c\": true, \"d\": false, \"e\": null, \"f\": {}}"));
        assertEquals(List.of(1L, 2.5, -3L), object.get("a"));
        assertEquals("x\nyA", object.get("b"));
        assertEquals(true, object.get("c"));
        assertEquals(false, object.get("d"));
        assertNull(object.get("e"));
        assertEquals(Map.of(), object.get("f"));
    }

    @Test
    void badTextIsRefused() {
        for (String bad : new String[] {"", "{", "[1,", "{\"a\" 1}", "{\"a\":1} x", "tru", "\"abc", "{\"a\":1,\"a\":2}", "\"\\q\""}) {
            assertThrows(IllegalArgumentException.class, () -> Json.parse(bad), bad);
        }
    }

    @Test
    void wrongTypesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Json.object(Json.parse("[]")));
        assertThrows(IllegalArgumentException.class, () -> Json.array(Json.parse("{}")));
        assertThrows(IllegalArgumentException.class, () -> Json.string(Json.parse("1")));
    }
}
