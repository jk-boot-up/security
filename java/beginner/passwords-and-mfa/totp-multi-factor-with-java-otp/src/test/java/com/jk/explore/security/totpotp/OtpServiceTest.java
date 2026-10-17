package com.jk.explore.security.totpotp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    private final SecretKey key = Secrets.key(Secrets.SHA1, "HmacSHA1");

    @Test
    void theLibraryValidatesOnlyTheStepTheTimeIsIn() {
        OtpService otp = OtpService.standard(1, false);
        String code = otp.codeAt(key, 59);
        assertTrue(otp.libraryValidates(key, 30, code));
        assertTrue(otp.libraryValidates(key, 59, code));
        assertFalse(otp.libraryValidates(key, 60, code));
        assertFalse(otp.libraryValidates(key, 29, code));
    }

    @Test
    void theBanksWindowOfOneAcceptsNeighbouringStepsButNotFurther() {
        String code = OtpService.standard(1, false).codeAt(key, 59); // step 1
        assertEquals(OtpService.Result.OK, OtpService.standard(1, false).verify("u", key, code, 29));  // step 0
        assertEquals(OtpService.Result.OK, OtpService.standard(1, false).verify("u", key, code, 59));  // step 1
        assertEquals(OtpService.Result.OK, OtpService.standard(1, false).verify("u", key, code, 65));  // step 2
        assertEquals(OtpService.Result.WRONG_CODE, OtpService.standard(1, false).verify("u", key, code, 95)); // step 3
    }

    @Test
    void aWindowOfZeroAcceptsOnlyTheCurrentStep() {
        String code = OtpService.standard(0, false).codeAt(key, 59);
        assertEquals(OtpService.Result.OK, OtpService.standard(0, false).verify("u", key, code, 59));
        assertEquals(OtpService.Result.WRONG_CODE, OtpService.standard(0, false).verify("u", key, code, 65));
    }

    @Test
    void withoutReplayProtectionACodeWorksTwice() {
        OtpService otp = OtpService.standard(1, false);
        String code = otp.codeAt(key, 59);
        assertEquals(OtpService.Result.OK, otp.verify("u", key, code, 59));
        assertEquals(OtpService.Result.OK, otp.verify("u", key, code, 64));
    }

    @Test
    void withReplayProtectionTheSecondUseIsRefused() {
        OtpService otp = OtpService.standard(1, true);
        String code = otp.codeAt(key, 59);
        assertEquals(OtpService.Result.OK, otp.verify("u", key, code, 59));
        assertEquals(OtpService.Result.REPLAYED, otp.verify("u", key, code, 64));
    }

    @Test
    void replayMemoryIsKeptPerCustomer() {
        OtpService otp = OtpService.standard(1, true);
        String code = otp.codeAt(key, 59);
        assertEquals(OtpService.Result.OK, otp.verify("dana", key, code, 59));
        assertEquals(OtpService.Result.OK, otp.verify("emil", key, code, 59));
    }

    @Test
    void aWrongCodeIsRefused() {
        assertEquals(OtpService.Result.WRONG_CODE, OtpService.standard(1, true).verify("u", key, "000000", 59));
    }
}
