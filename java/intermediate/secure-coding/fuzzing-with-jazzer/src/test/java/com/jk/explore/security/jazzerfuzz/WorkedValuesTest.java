package com.jk.explore.security.jazzerfuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The values the demo works out without Jazzer: the odds of a random product code, the minimizer's steps and the overflow. */
class WorkedValuesTest {

    @Test
    void aRandomProductCodeIsRare() {
        assertEquals(BigInteger.valueOf(409_977_207L), JazzerFuzzDemo.oneProductCodeIn());
        assertTrue(JazzerFuzzDemo.oneProductCodeIn().compareTo(BigInteger.valueOf(JazzerFuzzDemo.ORDER_RUNS)) > 0);
    }

    @Test
    void theMinimizerGoesShorterThenPlainerAndKeepsTheCrash() {
        List<String> steps = new ArrayList<>();
        String smallest = Minimizer.minimize(Parsers::orderLineCareless, "SHO-1042,28990", steps);
        assertEquals("AAA-0000,0", smallest);
        assertEquals(List.of("SHO-1042,90", "SHO-1042,0", "AHO-1042,0", "AAO-1042,0", "AAA-1042,0", "AAA-0042,0", "AAA-0002,0", "AAA-0000,0"), steps);
        for (String step : steps) {
            assertEquals("ArrayIndexOutOfBoundsException@OrderLine.java:25", Crashes.signature(Parsers::orderLineCareless, step));
        }
    }

    @Test
    void theLineNumberSaysWhichFieldIsMissing() {
        assertEquals("ArrayIndexOutOfBoundsException (line 24)", JazzerFuzzDemo.crash(Parsers::orderLineCareless, "SHO-1042"));
        assertEquals("ArrayIndexOutOfBoundsException (line 25)", JazzerFuzzDemo.crash(Parsers::orderLineCareless, "SHO-1042,2"));
    }

    @Test
    void theOverflowIsTheLow32BitsOfTheExactTotal() {
        long exact = 4000L * 4_000_000L;
        assertEquals(16_000_000_000L, exact);
        assertEquals(3_115_098_112L, exact - 3 * (1L << 32));
        assertEquals(-1_179_869_184, (int) exact);
    }

    @Test
    void theExamplesGiveTheirResults() {
        assertEquals("total 17980 cents", JazzerFuzzDemo.orderResult("SHO-1042,2,8990"));
        assertEquals("refused (bad product code)", JazzerFuzzDemo.orderResult(""));
    }
}
