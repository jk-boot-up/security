package com.jk.explore.security.totpotp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.crypto.SecretKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The library's codes against the published values of RFC 4226 and RFC 6238. */
class LibraryCodesTest {

    @ParameterizedTest
    @CsvSource({"0, 755224", "30, 287082", "60, 359152", "90, 969429", "120, 338314", "150, 254676", "180, 287922",
        "210, 162583", "240, 399871", "270, 520489"})
    void sixDigitSha1MatchesRfc4226(long time, String expected) {
        SecretKey key = Secrets.key(Secrets.SHA1, "HmacSHA1");
        assertEquals(expected, OtpService.standard(1, true).codeAt(key, time));
    }

    @ParameterizedTest
    @CsvSource({"59, 94287082", "1111111109, 07081804", "1111111111, 14050471", "1234567890, 89005924",
        "2000000000, 69279037", "20000000000, 65353130"})
    void eightDigitSha1MatchesRfc6238(long time, String expected) {
        OtpService otp = new OtpService(OtpService.generator(8, "HmacSHA1"), 1, true);
        assertEquals(expected, otp.codeAt(Secrets.key(Secrets.SHA1, "HmacSHA1"), time));
    }

    @ParameterizedTest
    @CsvSource({"59, 46119246", "1111111109, 68084774"})
    void eightDigitSha256MatchesRfc6238(long time, String expected) {
        OtpService otp = new OtpService(OtpService.generator(8, "HmacSHA256"), 1, true);
        assertEquals(expected, otp.codeAt(Secrets.key(Secrets.SHA256, "HmacSHA256"), time));
    }

    @ParameterizedTest
    @CsvSource({"59, 90693936", "1111111109, 25091201"})
    void eightDigitSha512MatchesRfc6238(long time, String expected) {
        OtpService otp = new OtpService(OtpService.generator(8, "HmacSHA512"), 1, true);
        assertEquals(expected, otp.codeAt(Secrets.key(Secrets.SHA512, "HmacSHA512"), time));
    }
}
