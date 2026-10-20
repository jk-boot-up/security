package com.jk.explore.security.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The example tests: what the parsers were tested with before any fuzzing. Both versions pass them. */
class ParsersTest {

    @Test
    void orderLinesParseAndPriceTheTotal() throws Exception {
        OrderLine line = OrderLine.parse("SHO-1042,2,8990", new Coverage());
        assertEquals("SHO-1042", line.sku());
        assertEquals(17980, line.total().cents());
        assertEquals(17980, OrderLine.parseCareless("SHO-1042,2,8990", new Coverage()).total().cents());
        assertEquals("SHO-1042,2,8990", line.print());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SHO-1042,two,8990", "sho-1042,2,8990", "SHO-1042,2,", "SHO-1042,0,8990", "SHO-1042,100,8990", "SHO-1042,2,0"})
    void badOrderLinesAreRefused(String input) {
        assertThrows(ParseException.class, () -> OrderLine.parse(input, new Coverage()));
    }

    @Test
    void cartsParseIntoValues() throws Exception {
        Value cart = CartParser.parse("{\"sku\":\"SHO-1042\",\"qty\":2,\"tags\":[\"gift\",-1]}", new Coverage());
        Value.Obj object = assertInstanceOf(Value.Obj.class, cart);
        assertEquals(new Value.Num(2), object.fields().get("qty"));
        assertEquals(new Value.Items(List.of(new Value.Str("gift"), new Value.Num(-1))), object.fields().get("tags"));
    }

    @Test
    void escapesRoundTrip() throws Exception {
        Value cart = CartParser.parse("{\"note\":\"#ana:say \\\"hi\\\" \\\\o/\"}", new Coverage());
        assertEquals(new Value.Obj(Map.of("note", new Value.Str("#ana:say \"hi\" \\o/"))), cart);
        assertEquals(cart, CartParser.parse(Value.print(cart), new Coverage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{\"sku\":}", "[1,]", "\"open", "{\"a\":1}x", "1234567890123456789", "\"\\n\"", "{\"note\":\"#:hi\"}"})
    void badCartsAreRefused(String input) {
        assertThrows(ParseException.class, () -> CartParser.parse(input, new Coverage()));
    }

    @Test
    void aGiftMessageNamesItsRecipient() throws Exception {
        assertInstanceOf(Value.Obj.class, CartParser.parse("{\"note\":\"#ana:happy birthday\"}", new Coverage()));
        assertInstanceOf(Value.Obj.class, CartParser.parse("{\"note\":\"no gift\"}", new Coverage()));
    }

    @Test
    void moneyIsNeverNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1));
    }
}
