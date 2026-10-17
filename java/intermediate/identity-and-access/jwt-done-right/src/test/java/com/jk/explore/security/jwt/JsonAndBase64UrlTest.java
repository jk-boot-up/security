package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonAndBase64UrlTest {

    @Test
    void base64UrlRoundTripsWithoutPadding() throws Exception {
        byte[] bytes = {(byte) 0xfb, (byte) 0xff, 0x00, 0x10};
        String text = Base64Url.encode(bytes);
        assertEquals("-_8AEA", text);
        assertArrayEquals(bytes, Base64Url.decode(text));
    }

    @Test
    void base64UrlRefusesPaddingAndOtherAlphabets() {
        for (String bad : List.of("ab==", "a+b/", "ab c", "a")) {
            assertThrows(InvalidTokenException.class, () -> Base64Url.decode(bad), bad);
        }
    }

    @Test
    void jsonReadsWhatItWrites() throws Exception {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("s", "quote \" backslash \\ newline \n tab \t");
        value.put("n", -42L);
        value.put("b", true);
        value.put("l", List.of("a", 1L));
        value.put("z", null);
        assertEquals(value, Json.parseObject(Json.write(value)));
    }

    @Test
    void jsonReadsEscapesAndSpaces() throws Exception {
        Map<String, Object> value = Json.parseObject(" { \"a\" : \"\\u0041\\/\" , \"b\" : [ ] , \"c\" : { } } ");
        assertEquals("A/", value.get("a"));
        assertEquals(List.of(), value.get("b"));
        assertEquals(Map.of(), value.get("c"));
    }

    @Test
    void jsonRefusesDuplicatesFractionsAndJunk() {
        for (String bad : List.of("{\"a\":1,\"a\":2}", "{\"exp\":1.5}", "{\"exp\":1e9}", "{\"a\":1} x", "[1]", "{\"a\":}", "{\"a\":\"\u0001\"}", "{\"a\":tru}")) {
            assertThrows(InvalidTokenException.class, () -> Json.parseObject(bad), bad);
        }
    }

    @Test
    void controlCharactersAreEscapedWhenWritten() throws Exception {
        String written = Json.write(Map.of("a", "\u0001"));
        assertEquals("{\"a\":\"\\u0001\"}", written);
        assertEquals("\u0001", Json.parseObject(written).get("a"));
        assertEquals("{}", new String(Base64Url.decode(Base64Url.encode("{}".getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8));
    }
}
