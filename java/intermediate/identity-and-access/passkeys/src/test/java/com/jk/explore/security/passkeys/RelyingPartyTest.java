package com.jk.explore.security.passkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Every check the relying party makes, each shown as an answer it refuses with its exact reason. */
class RelyingPartyTest {

    private static final String RP = "bank.example";
    private static final String ORIGIN = "https://bank.example";

    private SimClock clock;
    private RelyingParty bank;
    private Authenticator phone;
    private Browser browser;

    @BeforeEach
    void register() {
        clock = new SimClock();
        bank = new RelyingParty(RP, ORIGIN, clock);
        phone = new Authenticator("phone", false);
        browser = new Browser(phone);
        RelyingParty.Options options = bank.startRegistration("ana");
        assertTrue(bank.finishRegistration("ana", browser.create(ORIGIN, RP, options.challenge(), true).registration().orElseThrow(), "phone").accepted());
    }

    private Authenticator.Assertion answer(RelyingParty.Options options, boolean verified) {
        return browser.get(ORIGIN, RP, options.challenge(), options.allowed(), verified).assertion().orElseThrow();
    }

    private Authenticator.Assertion answerWith(String challenge, String type, String origin, String rpId, boolean verified) {
        return phone.get(rpId, List.of(), new ClientData(type, challenge, origin).json(), verified).orElseThrow();
    }

    @Test
    void theBankStoresOnlyAPublicKey() {
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        assertEquals(91, stored.publicKey.length);
        assertEquals(16, stored.id.length);
    }

    @Test
    void aFreshAnswerIsAcceptedOnceOnly() {
        RelyingParty.Options options = bank.startSignIn("ana", false);
        Authenticator.Assertion answer = answer(options, false);
        assertEquals("signed in as ana with phone", bank.finishSignIn("ana", answer).reason());
        assertEquals("unknown or already used challenge", bank.finishSignIn("ana", answer).reason());
        assertEquals(0, bank.pendingChallenges());
    }

    @Test
    void aChallengeLastsExactlyFiveMinutes() {
        RelyingParty.Options onTime = bank.startSignIn("ana", false);
        Authenticator.Assertion first = answer(onTime, false);
        clock.advance(300);
        assertTrue(bank.finishSignIn("ana", first).accepted());
        RelyingParty.Options late = bank.startSignIn("ana", false);
        Authenticator.Assertion second = answer(late, false);
        clock.advance(301);
        assertEquals("challenge expired", bank.finishSignIn("ana", second).reason());
    }

    @Test
    void aChallengeIssuedForAnotherCustomerIsRefused() {
        RelyingParty.Options forBen = bank.startSignIn("ben", false);
        assertEquals("challenge was issued for something else",
                bank.finishSignIn("ana", answerWith(forBen.challenge(), ClientData.GET, ORIGIN, RP, false)).reason());
    }

    @Test
    void aRegistrationAnswerCannotBeUsedToSignIn() {
        RelyingParty.Options options = bank.startSignIn("ana", false);
        assertEquals("type is webauthn.create, not webauthn.get",
                bank.finishSignIn("ana", answerWith(options.challenge(), ClientData.CREATE, ORIGIN, RP, false)).reason());
    }

    @Test
    void anotherOriginIsRefused() {
        RelyingParty.Options options = bank.startSignIn("ana", false);
        assertEquals("origin https://bank-example.test is not https://bank.example",
                bank.finishSignIn("ana", answerWith(options.challenge(), ClientData.GET, "https://bank-example.test", RP, false)).reason());
    }

    @Test
    void aSignatureForAnotherRelyingPartyIdIsRefused() {
        Authenticator other = new Authenticator("other", false);
        other.create("shop.example", new ClientData(ClientData.CREATE, "x", "https://shop.example").json(), true);
        RelyingParty.Options options = bank.startSignIn("ana", false);
        Authenticator.Assertion forOther = other.get("shop.example", List.of(),
                new ClientData(ClientData.GET, options.challenge(), ORIGIN).json(), false).orElseThrow();
        assertEquals("signed for another relying party id", bank.finishSignIn("ana", forOther).reason());
    }

    @Test
    void paymentsNeedUserVerification() {
        assertEquals("user verification required", bank.finishSignIn("ana", answer(bank.startSignIn("ana", true), false)).reason());
        assertTrue(bank.finishSignIn("ana", answer(bank.startSignIn("ana", true), true)).accepted());
        assertTrue(bank.finishSignIn("ana", answer(bank.startSignIn("ana", false), false)).accepted());
    }

    @Test
    void flagsAndSignatureAreBoundTogether() {
        Authenticator.Assertion answer = answer(bank.startSignIn("ana", false), false);
        byte[] data = answer.authenticatorData().clone();
        data[32] = 0;
        assertEquals("no user presence", bank.finishSignIn("ana",
                new Authenticator.Assertion(answer.credentialId(), data, answer.clientDataJson(), answer.signature())).reason());
    }

    @Test
    void aStrangersKeyUnderAnasCredentialIdIsRefused() {
        Authenticator other = new Authenticator("other", false);
        other.create(RP, new ClientData(ClientData.CREATE, "x", ORIGIN).json(), true);
        RelyingParty.Options options = bank.startSignIn("ana", false);
        Authenticator.Assertion forged = other.get(RP, List.of(), new ClientData(ClientData.GET, options.challenge(), ORIGIN).json(), false)
                .orElseThrow().withCredentialId(bank.credentialsOf("ana").getFirst().id);
        assertEquals("signature does not match", bank.finishSignIn("ana", forged).reason());
        RelyingParty.Options again = bank.startSignIn("ana", false);
        Authenticator.Assertion unknown = other.get(RP, List.of(), new ClientData(ClientData.GET, again.challenge(), ORIGIN).json(), false)
                .orElseThrow();
        assertEquals("no such passkey for ana", bank.finishSignIn("ana", unknown).reason());
    }

    @Test
    void aCopiedKeyIsCaughtByTheCounterAndSuspendsThePasskey() {
        assertTrue(bank.finishSignIn("ana", answer(bank.startSignIn("ana", false), false)).accepted());
        Authenticator copy = phone.copyAs("copy");
        assertTrue(bank.finishSignIn("ana", answer(bank.startSignIn("ana", false), false)).accepted());
        RelyingParty.Options options = bank.startSignIn("ana", false);
        Authenticator.Assertion fromCopy = new Browser(copy).get(ORIGIN, RP, options.challenge(), options.allowed(), false).assertion().orElseThrow();
        assertEquals("counter 2 is not above 2: possible cloned authenticator", bank.finishSignIn("ana", fromCopy).reason());
        assertEquals("passkey suspended after a counter went backwards", bank.finishSignIn("ana", answer(bank.startSignIn("ana", false), false)).reason());
    }

    @Test
    void syncedPasskeysReportZeroAndAreAccepted() {
        Authenticator synced = new Authenticator("synced", true);
        Browser other = new Browser(synced);
        RelyingParty.Options options = bank.startRegistration("ana");
        bank.finishRegistration("ana", other.create(ORIGIN, RP, options.challenge(), true).registration().orElseThrow(), "synced");
        for (int i = 0; i < 3; i++) {
            RelyingParty.Options signIn = bank.startSignIn("ana", false);
            Authenticator.Assertion answer = other.get(ORIGIN, RP, signIn.challenge(), signIn.allowed(), false).assertion().orElseThrow();
            assertEquals(0, AuthenticatorData.decode(answer.authenticatorData()).signCount());
            assertTrue(bank.finishSignIn("ana", answer).accepted());
        }
    }

    @Test
    void theSameCredentialCannotBeRegisteredTwice() {
        RelyingParty.Options options = bank.startRegistration("ben");
        Authenticator.Registration registration = browser.create(ORIGIN, RP, options.challenge(), true).registration().orElseThrow();
        assertTrue(bank.finishRegistration("ben", registration, "phone").accepted());
        RelyingParty.Options again = bank.startRegistration("ben");
        Authenticator.Registration replayed = new Authenticator.Registration(registration.credentialId(), registration.publicKey(),
                registration.authenticatorData(), new ClientData(ClientData.CREATE, again.challenge(), ORIGIN).json());
        assertEquals("credential already registered", bank.finishRegistration("ben", replayed, "phone").reason());
    }

    @ParameterizedTest
    @CsvSource({
        "https://bank.example, bank.example, true",
        "https://login.bank.example, bank.example, true",
        "https://BANK.example, bank.example, true",
        "https://bank-example.test, bank.example, false",
        "https://evilbank.example, bank.example, false",
        "http://bank.example, bank.example, false",
        "https://bank.example, example, true"
    })
    void theBrowserOnlyUsesTheRelyingPartyIdOfThePagesOwnSite(String origin, String rpId, boolean allowed) {
        assertEquals(allowed, Browser.rpIdAllowedFor(origin, rpId));
    }

    @Test
    void aLookAlikeSiteGetsNothing() {
        assertFalse(browser.get("https://bank-example.test", "bank-example.test", "c2l0ZQ", List.of(), false).ok());
        assertFalse(browser.get("https://bank-example.test", RP, "c2l0ZQ", List.of(), false).ok());
    }

    @Test
    void passwordsCanBeSwitchedOffAndRecoveryCodesWorkOnce() {
        bank.setPassword("ana", "Tulip-2026".toCharArray());
        assertTrue(bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).accepted());
        assertEquals("wrong username or password", bank.passwordSignIn("ana", "tulip-2026".toCharArray()).reason());
        bank.disablePassword("ana");
        assertFalse(bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).accepted());
        String code = bank.issueRecoveryCode("ana");
        assertEquals(20, code.length());
        assertTrue(bank.recover("ana", code).accepted());
        assertFalse(bank.recover("ana", code).accepted());
        assertFalse(bank.recover("ben", code).accepted());
    }
}
