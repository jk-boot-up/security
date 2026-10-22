package com.jk.explore.security.jazzerfuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The reader of Jazzer's report, on text in the shape Jazzer prints. */
class ReportTest {

    private static final String REPORT = """
            #3	INITED cov: 10 ft: 10 corp: 1/15b exec/s: 0 rss: 727Mb
            == Java Exception: java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
            	at com.jk.explore.security.jazzerfuzz.OrderLine.parseCareless(OrderLine.java:24)
            	at com.jk.explore.security.jazzerfuzz.Parsers.orderLineCareless(Parsers.java:11)
            	at com.jk.explore.security.jazzerfuzz.targets.OrderLineCareless.fuzzerTestOneInput(OrderLineCareless.java:10)
            DEDUP_TOKEN: 27fd453f4ace8d53
            artifact_prefix='/tmp/a/'; Test unit written to /tmp/a/crash-1
            == Java Exception: java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
            	at com.jk.explore.security.jazzerfuzz.OrderLine.parseCareless(OrderLine.java:24)
            DEDUP_TOKEN: aaaaaaaaaaaaaaaa
            artifact_prefix='/tmp/a/'; Test unit written to /tmp/a/crash-2
            == Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: Regular Expression Injection
            	at com.code_intelligence.jazzer.sanitizers.RegexInjection.hookInternal(RegexInjection.kt:164)
            	at com.jk.explore.security.jazzerfuzz.Parsers.searchCareless(Parsers.java:43)
            DEDUP_TOKEN: bbbbbbbbbbbbbbbb
            artifact_prefix='/tmp/a/'; Test unit written to /tmp/a/crash-3
            #300000	DONE   cov: 19 ft: 26 corp: 9/107b lim: 2996 exec/s: 300000 rss: 783Mb
            """;

    @Test
    void findingsAreDistinctByKindAndPlaceAndKeepTheirInputFile() {
        JazzerRun.Result result = JazzerRun.parse(REPORT);
        assertEquals(2, result.findings().size());
        assertEquals("ArrayIndexOutOfBoundsException in OrderLine.parseCareless (line 24)", result.findings().get(0).describe());
        assertEquals("/tmp/a/crash-1", result.findings().get(0).input().toString());
        assertEquals("Regular Expression Injection in Parsers.searchCareless (line 43)", result.findings().get(1).describe());
    }

    @Test
    void theRunsAndTheCoverageAreReadFromTheLastLine() {
        JazzerRun.Result result = JazzerRun.parse(REPORT);
        assertEquals(300000, result.runs());
        assertEquals(19, result.coverage());
    }
}
