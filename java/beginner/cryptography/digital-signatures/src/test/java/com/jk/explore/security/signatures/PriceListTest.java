package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PriceListTest {

    @Test
    void theEncodingSortsProductsSoOrderDoesNotMatter() {
        Map<String, Integer> a = new LinkedHashMap<>();
        a.put("mouse", 25);
        a.put("laptop", 1200);
        Map<String, Integer> b = new LinkedHashMap<>();
        b.put("laptop", 1200);
        b.put("mouse", 25);
        assertArrayEquals(new PriceList(2, 100, a).encode(), new PriceList(2, 100, b).encode());
    }

    @Test
    void theEncodingIsOneProductPerLineWithVersionAndExpiryFirst() {
        String text = new String(SignaturesDemo.list(2, 100, 1200, 25).encode(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("version=2\nvalidUntilDay=100\nlaptop=1200\nmouse=25\n", text);
    }

    @Test
    void anyChangeToAPriceTheVersionOrTheExpiryChangesTheBytes() {
        byte[] base = SignaturesDemo.list(2, 100, 1200, 25).encode();
        assertFalse(java.util.Arrays.equals(base, SignaturesDemo.list(2, 100, 12, 25).encode()));
        assertFalse(java.util.Arrays.equals(base, SignaturesDemo.list(3, 100, 1200, 25).encode()));
        assertFalse(java.util.Arrays.equals(base, SignaturesDemo.list(2, 101, 1200, 25).encode()));
    }
}
