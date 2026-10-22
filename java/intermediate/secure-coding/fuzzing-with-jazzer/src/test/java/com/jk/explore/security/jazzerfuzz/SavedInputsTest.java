package com.jk.explore.security.jazzerfuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** The inputs Jazzer found, made smallest, against the careless and the fixed parsers. */
class SavedInputsTest {

    @Test
    void aLineWithOnlyAProductCodeCrashesTheCarelessParserAtTheSecondField() {
        assertEquals("ArrayIndexOutOfBoundsException@OrderLine.java:24", Crashes.signature(Parsers::orderLineCareless, "AAA-0000"));
        assertNull(Crashes.signature(Parsers::orderLine, "AAA-0000"));
    }

    @Test
    void aLineWithTwoFieldsCrashesItAtTheThird() {
        assertEquals("ArrayIndexOutOfBoundsException@OrderLine.java:25", Crashes.signature(Parsers::orderLineCareless, "AAA-0000,0"));
        assertNull(Crashes.signature(Parsers::orderLine, "AAA-0000,0"));
    }

    @Test
    void aTotalThatOverflowsAnIntBecomesANegativeMoney() {
        assertEquals("IllegalArgumentException@Money.java:8", Crashes.signature(Parsers::orderLineCareless, "AAA-0000,4000,4000000"));
        assertNull(Crashes.signature(Parsers::orderLine, "AAA-0000,4000,4000000"));
    }

    @Test
    void aGiftMessageWithoutARecipientCrashesTheCarelessCartParser() {
        assertEquals("StringIndexOutOfBoundsException@CartParser.java:150", Crashes.signature(Parsers::cartCareless, "[{\"note\":\"#\"}]"));
        assertNull(Crashes.signature(Parsers::cart, "[{\"note\":\"#\"}]"));
    }

    @Test
    void deepNestingGivesWayTheCarelessStackAndIsRefusedByTheFixedParser() {
        String nested = "[".repeat(100_000);
        assertEquals("StackOverflowError", Crashes.signature(Parsers::cartCareless, nested));
        ParseException e = assertThrows(ParseException.class, () -> CartParser.parse(nested));
        assertEquals("nested deeper than 32", e.getMessage());
    }

    @Test
    void theRegularExpressionBoxTakesTheCustomersWordsAsAPatternUntilItIsFixed() {
        assertThrows(java.util.regex.PatternSyntaxException.class, () -> Parsers.searchCareless("("));
        assertEquals(false, Parsers.search("("));
    }

    @Test
    void anExampleInputStillWorksOnBothVersions() throws Exception {
        assertEquals(17980, OrderLine.parse("SHO-1042,2,8990").total().cents());
        assertEquals(17980, OrderLine.parseCareless("SHO-1042,2,8990").total().cents());
    }
}
