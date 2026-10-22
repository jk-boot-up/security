package com.jk.explore.security.jazzerfuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The minimizer on inputs shaped like the ones Jazzer finds. */
class MinimizerTest {

    @Test
    void aLongRandomLookingLineShrinksToTheShortestOneWithTheSameCrash() {
        assertEquals("AAA-0000", Minimizer.minimize(Parsers::orderLineCareless, "SHO-1042"));
        assertEquals("AAA-0000", Minimizer.minimize(Parsers::orderLineCareless, "QRS-7731"));
        assertEquals("AAA-0000,0", Minimizer.minimize(Parsers::orderLineCareless, "BAG-0007,12345"));
    }

    @Test
    void anInputThatDoesNotCrashIsReturnedAsItIs() {
        assertEquals("SHO-1042,2,8990", Minimizer.minimize(Parsers::orderLineCareless, "SHO-1042,2,8990"));
    }

    @Test
    void theGiftMessageCrashShrinksToAnUnfinishedObjectWithANote() {
        String smallest = Minimizer.minimize(Parsers::cartCareless, "[{\"sku\":\"SHO-1042\",\"qty\":2,\"note\":\"#hello\"}]");
        assertEquals("{\"note\":\"#\"", smallest);
    }
}
