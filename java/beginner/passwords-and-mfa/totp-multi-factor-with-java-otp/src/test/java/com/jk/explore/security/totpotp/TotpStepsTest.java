package com.jk.explore.security.totpotp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The code worked out by hand is exactly the library's code. */
class TotpStepsTest {

    @ParameterizedTest
    @ValueSource(longs = {0, 30, 59, 60, 90, 1111111109, 2000000000})
    void handMadeCodeEqualsTheLibrarysCode(long time) {
        String library = OtpService.standard(1, true).codeAt(Secrets.key(Secrets.SHA1, "HmacSHA1"), time);
        assertEquals(library, TotpSteps.at(Secrets.SHA1, time).code());
    }

    @Test
    void stepsAtTime59MatchTheWorkedExampleOfRfc4226() {
        TotpSteps.Steps s = TotpSteps.at(Secrets.SHA1, 59);
        assertEquals(1, s.step());
        assertEquals(8, s.counter().length);
        assertEquals(20, s.hmac().length);
        assertEquals(s.hmac()[19] & 0x0f, s.offset());
        assertEquals(s.fourBytes() & 0x7fffffff, s.number());
        assertEquals("287082", s.code());
    }
}
