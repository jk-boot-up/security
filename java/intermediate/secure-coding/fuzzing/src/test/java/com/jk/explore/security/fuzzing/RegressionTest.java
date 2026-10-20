package com.jk.explore.security.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The smallest crashing inputs the fuzzer and the minimiser found, kept as tests. Each one crashed the careless parser with an
 * unexpected exception; the fixed parser must refuse it as invalid input, with a ParseException and nothing else.
 */
class RegressionTest {

    @ParameterizedTest
    @ValueSource(strings = {"SHO-1042", "BAG-0007,999999999,4", "SHO-1042,2,8990,extra", "SHO-1042,3000000,1000"})
    void orderLinesThatOnceCrashedAreRefused(String input) {
        assertThrows(ParseException.class, () -> OrderLine.parse(input, new Coverage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"note\":\"#\"}", "{\"note\":\"#ana\"}"})
    void giftMessagesThatOnceCrashedAreRefused(String input) {
        assertThrows(ParseException.class, () -> CartParser.parse(input, new Coverage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SHO-1042", "BAG-0007,999999999,4"})
    void theCarelessOrderParserStillCrashesOnThem(String input) {
        assertEquals(null, Fuzzer.execute(Harness.ORDERS, input).failure());
        assertEquals(true, Fuzzer.execute(Harness.CARELESS_ORDERS, input).failure() != null);
    }

    @org.junit.jupiter.api.Test
    void deepNestingIsRefusedNotOverflowed() {
        String deep = "[".repeat(Fuzzer.DEEP_NESTING) + "1" + "]".repeat(Fuzzer.DEEP_NESTING);
        assertThrows(ParseException.class, () -> CartParser.parse(deep, new Coverage()));
        assertEquals(StackOverflowError.class, Fuzzer.execute(Harness.CARELESS_CARTS, deep).failure().getClass());
    }
}
