package com.jk.explore.security.jazzerfuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Jazzer started as a program. Run with ./gradlew integrationTest. */
@Tag("integration")
class JazzerRealTest {

    @Test
    void withTheExampleLinesAsSeedsItFindsTheThreeBugsAndFixedCodeHasNone() throws Exception {
        JazzerRun.Result careless = JazzerFuzzDemo.fuzz("OrderLineCareless", JazzerFuzzDemo.ORDER_SEEDS, 300_000, 4096);
        List<String> names = careless.findings().stream().map(Finding::describe).sorted().toList();
        assertEquals(List.of("ArrayIndexOutOfBoundsException in OrderLine.parseCareless (line 24)", "ArrayIndexOutOfBoundsException in OrderLine.parseCareless (line 25)", "IllegalArgumentException in Money.<init> (line 8)"), names);
        assertEquals(0, JazzerFuzzDemo.fuzz("OrderLineFixed", JazzerFuzzDemo.ORDER_SEEDS, 300_000, 4096).findings().size());
    }

    @Test
    void withNoSeedsItCannotGuessAProductCode() throws Exception {
        JazzerRun.Result result = JazzerFuzzDemo.fuzz("OrderLineCareless", List.of(), 300_000, 4096);
        assertEquals(0, result.findings().size());
        assertTrue(result.coverage() < 10);
    }

    @Test
    void theRegularExpressionSanitizerFlagsTheSearchBox() throws Exception {
        JazzerRun.Result result = JazzerFuzzDemo.fuzz("SearchBoxCareless", JazzerFuzzDemo.SEARCH_SEEDS, 300_000, 4096);
        assertTrue(result.findings().stream().anyMatch(f -> f.exception().equals("Regular Expression Injection")));
        assertEquals(0, JazzerFuzzDemo.fuzz("SearchBoxFixed", JazzerFuzzDemo.SEARCH_SEEDS, 300_000, 4096).findings().size());
    }

    @Test
    void theGiftMessageBugNeedsASeedWithANoteField() throws Exception {
        assertEquals(0, JazzerFuzzDemo.fuzz("CartCareless", JazzerFuzzDemo.CART_SEEDS, 1_000_000, 4096).findings().size());
        assertTrue(JazzerFuzzDemo.fuzz("CartCareless", JazzerFuzzDemo.CART_SEEDS_WITH_NOTE, 1_000_000, 4096).findings().stream().anyMatch(f -> f.where().startsWith("CartParser.giftMessage")));
    }
}
