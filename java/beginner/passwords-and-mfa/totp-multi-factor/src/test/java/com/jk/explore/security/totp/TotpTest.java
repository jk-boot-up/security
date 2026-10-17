package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The test vectors of RFC 6238, appendix B, with 8-digit codes. */
class TotpTest {

    private static final byte[] SHA1_KEY = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SHA256_KEY = "12345678901234567890123456789012".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SHA512_KEY =
            "1234567890123456789012345678901234567890123456789012345678901234".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({"59, 94287082", "1111111109, 07081804", "1111111111, 14050471", "1234567890, 89005924",
        "2000000000, 69279037", "20000000000, 65353130"})
    void sha1MatchesTheRfc(long time, String expected) {
        assertEquals(expected, Totp.codeAt(SHA1_KEY, time, 8, "HmacSHA1"));
    }

    @ParameterizedTest
    @CsvSource({"59, 46119246", "1111111109, 68084774"})
    void sha256MatchesTheRfc(long time, String expected) {
        assertEquals(expected, Totp.codeAt(SHA256_KEY, time, 8, "HmacSHA256"));
    }

    @ParameterizedTest
    @CsvSource({"59, 90693936", "1111111109, 25091201"})
    void sha512MatchesTheRfc(long time, String expected) {
        assertEquals(expected, Totp.codeAt(SHA512_KEY, time, 8, "HmacSHA512"));
    }

    @Test
    void theCounterIsTheNumberOfWholeThirtySecondSteps() {
        assertEquals(0, Totp.counterAt(0));
        assertEquals(0, Totp.counterAt(29));
        assertEquals(1, Totp.counterAt(30));
        assertEquals(1, Totp.counterAt(59));
        assertEquals(2, Totp.counterAt(60));
    }

    @Test
    void theCodeChangesEveryStepAndNotWithinOne() {
        assertEquals(Totp.codeAt(SHA1_KEY, 30), Totp.codeAt(SHA1_KEY, 59));
        assertEquals("755224", Totp.codeAt(SHA1_KEY, 29));
        assertEquals("287082", Totp.codeAt(SHA1_KEY, 30));
    }
}
