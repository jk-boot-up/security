package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MfaServerTest {

    private final byte[] key = DemoSecrets.RFC_KEY;

    private MfaServer server(int window, boolean replay) {
        MfaServer s = TotpDemo.bank(window, replay);
        s.enroll(TotpDemo.USER);
        return s;
    }

    private Verdict login(MfaServer s, String code, long now) {
        return s.login(TotpDemo.USER, TotpDemo.PASSWORD, code, now);
    }

    @Test
    void aWrongPasswordIsRejectedWhateverTheCode() {
        MfaServer s = server(1, true);
        assertEquals(Verdict.WRONG_PASSWORD, s.login(TotpDemo.USER, "nope", Totp.codeAt(key, 59), 59));
    }

    @Test
    void aCustomerWithoutASecondFactorLogsInWithThePasswordAlone() {
        MfaServer s = TotpDemo.bank(1, true);
        assertEquals(Verdict.OK, s.login(TotpDemo.USER, TotpDemo.PASSWORD, null, 59));
    }

    @Test
    void anEnrolledCustomerWithNoCodeIsAskedForOne() {
        assertEquals(Verdict.CODE_NEEDED, login(server(1, true), null, 59));
    }

    @Test
    void theCurrentCodeIsAccepted() {
        assertEquals(Verdict.OK, login(server(1, true), Totp.codeAt(key, 59), 59));
    }

    @Test
    void aWindowOfOneAcceptsThePreviousCurrentAndNextStepOnly() {
        long now = 120; // step 4
        assertEquals(Verdict.WRONG_CODE, login(server(1, false), Hotp.code(key, 2, 6), now));
        assertEquals(Verdict.OK, login(server(1, false), Hotp.code(key, 3, 6), now));
        assertEquals(Verdict.OK, login(server(1, false), Hotp.code(key, 4, 6), now));
        assertEquals(Verdict.OK, login(server(1, false), Hotp.code(key, 5, 6), now));
        assertEquals(Verdict.WRONG_CODE, login(server(1, false), Hotp.code(key, 6, 6), now));
    }

    @Test
    void aWindowOfZeroAcceptsOnlyTheCurrentStep() {
        assertEquals(Verdict.WRONG_CODE, login(server(0, false), Hotp.code(key, 3, 6), 120));
        assertEquals(Verdict.OK, login(server(0, false), Hotp.code(key, 4, 6), 120));
    }

    @Test
    void aCodeCanBeReusedWithoutReplayProtection() {
        MfaServer s = server(1, false);
        String code = Totp.codeAt(key, 59);
        assertEquals(Verdict.OK, login(s, code, 59));
        assertEquals(Verdict.OK, login(s, code, 64));
    }

    @Test
    void aCodeCannotBeReusedWithReplayProtection() {
        MfaServer s = server(1, true);
        String code = Totp.codeAt(key, 59);
        assertEquals(Verdict.OK, login(s, code, 59));
        assertEquals(Verdict.REPLAYED, login(s, code, 64));
    }

    @Test
    void aLaterCodeIsAcceptedAfterAnEarlierOne() {
        MfaServer s = server(1, true);
        assertEquals(Verdict.OK, login(s, Totp.codeAt(key, 59), 59));
        assertEquals(Verdict.OK, login(s, Totp.codeAt(key, 90), 90));
    }

    @Test
    void anOlderCodeIsRefusedOnceANewerOneWasUsed() {
        MfaServer s = server(1, true);
        assertEquals(Verdict.OK, login(s, Totp.codeAt(key, 90), 90));                // step 3
        assertEquals(Verdict.REPLAYED, login(s, Totp.codeAt(key, 75), 90));          // step 2, still in the window
    }

    @Test
    void theServerHoldsTheSecretSoATheftOfItsTableYieldsTheCodes() {
        MfaServer s = server(1, true);
        assertArrayEquals(key, s.stolenSecrets().get(TotpDemo.USER));
    }

    @Test
    void recoveryCodesWorkOnce() {
        MfaServer s = server(1, true);
        List<String> codes = s.issueRecoveryCodes(TotpDemo.USER, 3);
        assertEquals(3, codes.size());
        assertEquals(Verdict.OK, s.loginWithRecoveryCode(TotpDemo.USER, TotpDemo.PASSWORD, codes.get(0)));
        assertEquals(Verdict.WRONG_CODE, s.loginWithRecoveryCode(TotpDemo.USER, TotpDemo.PASSWORD, codes.get(0)));
        assertEquals(Verdict.OK, s.loginWithRecoveryCode(TotpDemo.USER, TotpDemo.PASSWORD, codes.get(1)));
    }

    @Test
    void aRecoveryCodeNeedsTheRightPasswordToo() {
        MfaServer s = server(1, true);
        List<String> codes = s.issueRecoveryCodes(TotpDemo.USER, 1);
        assertEquals(Verdict.WRONG_PASSWORD, s.loginWithRecoveryCode(TotpDemo.USER, "nope", codes.get(0)));
    }

    @Test
    void secureSecretsAreTwentyRandomBytes() {
        SecureSecrets secure = new SecureSecrets();
        assertEquals(20, secure.newSecret().length);
        assertEquals(false, java.util.Arrays.equals(secure.newSecret(), secure.newSecret()));
    }
}
