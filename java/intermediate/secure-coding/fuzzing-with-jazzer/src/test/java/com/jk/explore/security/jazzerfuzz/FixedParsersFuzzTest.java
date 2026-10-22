package com.jk.explore.security.jazzerfuzz;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;

/**
 * Fuzz tests for the fixed parsers. In an ordinary build they run in regression mode: every saved input under
 * src/test/resources/.../FixedParsersFuzzTestInputs/ is replayed, which takes milliseconds. With the environment variable JAZZER_FUZZ=1 each runs as a real fuzz test
 * for the time given and writes new findings next to the saved ones.
 */
class FixedParsersFuzzTest {

    @FuzzTest(maxDuration = "20s")
    void orderLine(FuzzedDataProvider data) {
        Parsers.orderLine(data.consumeRemainingAsAsciiString());
    }

    @FuzzTest(maxDuration = "20s")
    void cart(FuzzedDataProvider data) {
        Parsers.cart(data.consumeRemainingAsAsciiString());
    }

    @FuzzTest(maxDuration = "20s")
    void search(FuzzedDataProvider data) {
        Parsers.search(data.consumeRemainingAsAsciiString());
    }
}
