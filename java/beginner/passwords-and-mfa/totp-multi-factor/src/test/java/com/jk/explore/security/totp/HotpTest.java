package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HotpTest {

    /** The test values of RFC 4226, appendix D, for the key 12345678901234567890. */
    @ParameterizedTest
    @CsvSource({"0, 755224", "1, 287082", "2, 359152", "3, 969429", "4, 338314", "5, 254676", "6, 287922",
        "7, 162583", "8, 399871", "9, 520489"})
    void matchesTheRfcValues(long counter, String expected) {
        assertEquals(expected, Hotp.code(DemoSecrets.RFC_KEY, counter, 6));
    }

    @org.junit.jupiter.api.Test
    void aCodeKeepsItsLeadingZeros() {
        String code = Totp.codeAt(DemoSecrets.RFC_KEY, 1111111109L, 8, "HmacSHA1");
        assertEquals("07081804", code);
    }
}
