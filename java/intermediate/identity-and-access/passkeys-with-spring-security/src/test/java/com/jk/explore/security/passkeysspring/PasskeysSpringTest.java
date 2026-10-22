package com.jk.explore.security.passkeysspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

/** The real Spring Security WebAuthn endpoints in a real bank on a loopback port, with an authenticator in software. */
class PasskeysSpringTest {

    private static final String ORIGIN = PasskeysSpringDemo.ORIGIN;
    private static final String RP = PasskeysSpringDemo.RP;

    private static SoftwareAuthenticator register(Running bank, String user) throws Exception {
        PasskeyBrowser b = bank.browser();
        b.signInWithPassword(user, PasskeySecurity.PASSWORD);
        SoftwareAuthenticator a = new SoftwareAuthenticator();
        assertEquals(200, b.registerPasskey(a, user + "'s phone", true, ORIGIN, RP).status());
        return a;
    }

    @Test
    void registrationNeedsASignedInCustomer() throws Exception {
        try (Running bank = Running.start()) {
            assertNotEquals(200, bank.browser().send("POST", "/webauthn/register/options", "application/json", "{}").status());
        }
    }

    @Test
    void aPostWithoutTheCsrfTokenIsRefused() throws Exception {
        try (Running bank = Running.start()) {
            PasskeyBrowser b = bank.browser();
            b.signInWithPassword("ana", PasskeySecurity.PASSWORD);
            assertEquals(403, b.send("POST", "/webauthn/register/options", "application/json", "{}", false).status());
            assertEquals(200, b.send("POST", "/webauthn/register/options", "application/json", "{}", true).status());
        }
    }

    @Test
    void theBankStoresAPublicKeyAndACounterOfZero() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            var record = bank.credentials().findByCredentialId(Bytes.fromBase64(phone.credentialId()));
            assertEquals(0, record.getSignatureCount());
            assertEquals(77, record.getPublicKey().getBytes().length);
            assertEquals("ana's phone", record.getLabel());
            assertFalse(record.isBackupEligible());
        }
    }

    @Test
    void aPasskeySignsInAndTheBankSeesAPasskeyNotAPassword() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            PasskeyBrowser b = bank.browser();
            assertEquals(200, b.signInWithPasskey(phone, true, ORIGIN, RP).status());
            assertEquals("WebAuthnAuthentication", b.me().json().get("signedInWith"));
            assertEquals("ana", b.me().json().get("name"));
        }
    }

    @Test
    void anAnswerWorksOnceAndOnlyForTheBrowserThatAskedForTheChallenge() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            PasskeyBrowser asker = bank.browser();
            SoftwareAuthenticator.Assertion a = phone.sign(RP, ORIGIN, String.valueOf(asker.authenticationOptions().get("challenge")), true, true);
            assertEquals(401, bank.browser().postAssertion(a).status());
            assertEquals(200, asker.postAssertion(a).status());
            assertEquals(401, asker.postAssertion(a).status());
        }
    }

    @Test
    void anotherKeyUnderTheSameCredentialIdIsRefused() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            assertEquals(401, bank.browser().signInWithPasskey(phone.impostor(), true, ORIGIN, RP).status());
        }
    }

    @Test
    void theBankChecksTheOriginAndTheRelyingPartyIdHash() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            assertEquals(401, bank.browser().signInWithPasskeyWithoutBrowserRules(phone, true, "https://bank-example.test", RP).status());
            assertEquals(401, bank.browser().signInWithPasskeyWithoutBrowserRules(phone, true, ORIGIN, "bank-example.test").status());
            assertEquals(200, bank.browser().signInWithPasskeyWithoutBrowserRules(phone, true, ORIGIN, RP).status());
        }
    }

    @Test
    void theBrowserAndTheAuthenticatorHaveRulesOfTheirOwn() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            PasskeyBrowser b = bank.browser();
            assertTrue(b.signInWithPasskey(phone, true, "https://bank-example.test", RP).blocked());
            assertTrue(b.signInWithPasskey(phone, true, "https://bank-example.test", "bank-example.test").blocked());
            assertTrue(b.signInWithPasskey(SoftwareAuthenticator.empty(), true, ORIGIN, RP).blocked());
            assertFalse(b.signInWithPasskey(phone, true, ORIGIN, RP).blocked());
        }
    }

    @Test
    void anAuthenticatorDataFlagChangedAfterSigningBreaksTheSignature() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            PasskeyBrowser b = bank.browser();
            SoftwareAuthenticator.Assertion a = phone.sign(RP, ORIGIN, String.valueOf(b.authenticationOptions().get("challenge")), false, true);
            byte[] data = B64.decode(a.authenticatorData());
            data[32] |= 0x04;
            assertEquals(401, b.postAssertion(new SoftwareAuthenticator.Assertion(a.credentialId(), B64.encode(data), a.clientDataJson(), a.signature(), a.userHandle())).status());
        }
    }

    @Test
    void springSecurityByDefaultAcceptsATouchAndABackwardsCounterAndAClone() throws Exception {
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = register(bank, "ana");
            assertEquals(200, bank.browser().signInWithPasskey(phone, false, ORIGIN, RP).status());
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            phone.setCounter(0);
            assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            SoftwareAuthenticator clone = phone.cloneIt();
            assertEquals(200, bank.browser().signInWithPasskey(clone, true, ORIGIN, RP).status());
        }
    }

    @Test
    void theHardenedBankRequiresUserVerification() throws Exception {
        try (Running bank = Running.start("bank.mode=hardened")) {
            SoftwareAuthenticator phone = register(bank, "ana");
            assertEquals(401, bank.browser().signInWithPasskey(phone, false, ORIGIN, RP).status());
            assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            assertEquals("user verification required", ((HardenedRelyingParty) bank.context().getBean(WebAuthnRelyingPartyOperations.class)).refusals().getFirst());
        }
    }

    @Test
    void theHardenedBankAsksForUserVerificationInItsOptions() throws Exception {
        try (Running bank = Running.start("bank.mode=hardened")) {
            assertEquals("required", bank.browser().authenticationOptions().get("userVerification"));
        }
        try (Running bank = Running.start()) {
            assertEquals("preferred", bank.browser().authenticationOptions().get("userVerification"));
        }
    }

    @Test
    void theHardenedBankSuspendsAClonedPasskeyForEveryoneIncludingTheRealPhone() throws Exception {
        try (Running bank = Running.start("bank.mode=hardened")) {
            SoftwareAuthenticator phone = register(bank, "ana");
            for (int i = 0; i < 3; i++) {
                assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            }
            SoftwareAuthenticator clone = phone.cloneIt();
            assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            assertEquals(401, bank.browser().signInWithPasskey(clone, true, ORIGIN, RP).status());
            assertEquals(401, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            HardenedRelyingParty rp = (HardenedRelyingParty) bank.context().getBean(WebAuthnRelyingPartyOperations.class);
            assertEquals("counter 4 is not above 4: possible cloned authenticator", rp.refusals().get(rp.refusals().size() - 2));
            assertEquals("passkey suspended after a counter went backwards", rp.refusals().getLast());
            assertTrue(rp.isSuspended(Bytes.fromBase64(phone.credentialId())));
        }
    }

    @Test
    void aForgedSignatureNeverSuspendsTheRealPasskey() throws Exception {
        try (Running bank = Running.start("bank.mode=hardened")) {
            SoftwareAuthenticator phone = register(bank, "ana");
            assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            SoftwareAuthenticator impostor = phone.impostor();
            impostor.setCounter(0);
            assertEquals(401, bank.browser().signInWithPasskey(impostor, true, ORIGIN, RP).status());
            assertEquals(200, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
        }
    }

    @Test
    void aSyncedPasskeyWithACounterOfZeroIsLetThrough() throws Exception {
        try (Running bank = Running.start("bank.mode=hardened")) {
            PasskeyBrowser ben = bank.browser();
            ben.signInWithPassword("ben", PasskeySecurity.PASSWORD);
            SoftwareAuthenticator synced = new SoftwareAuthenticator().synced();
            assertEquals(200, ben.registerPasskey(synced, "ben's synced passkey", true, ORIGIN, RP).status());
            assertTrue(bank.credentials().findByCredentialId(Bytes.fromBase64(synced.credentialId())).isBackupEligible());
            assertEquals(200, bank.browser().signInWithPasskey(synced, true, ORIGIN, RP).status());
            assertEquals(200, bank.browser().signInWithPasskey(synced, true, ORIGIN, RP).status());
        }
    }

    @Test
    void removingAPasskeyEndsItsUse() throws Exception {
        try (Running bank = Running.start()) {
            PasskeyBrowser ben = bank.browser();
            ben.signInWithPassword("ben", PasskeySecurity.PASSWORD);
            SoftwareAuthenticator phone = new SoftwareAuthenticator();
            SoftwareAuthenticator key = new SoftwareAuthenticator();
            ben.registerPasskey(phone, "phone", true, ORIGIN, RP);
            ben.registerPasskey(key, "key", true, ORIGIN, RP);
            assertEquals(204, ben.send("DELETE", "/webauthn/register/" + phone.credentialId(), null, null).status());
            assertEquals(401, bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).status());
            assertEquals(200, bank.browser().signInWithPasskey(key, true, ORIGIN, RP).status());
        }
    }

    @Test
    void thePasswordIsSwitchedOffOnlyForACustomerWithTwoPasskeys() throws Exception {
        try (Running bank = Running.start()) {
            PasskeyBrowser ana = bank.browser();
            ana.signInWithPassword("ana", PasskeySecurity.PASSWORD);
            ana.registerPasskey(new SoftwareAuthenticator(), "one", true, ORIGIN, RP);
            assertEquals(200, bank.browser().signInWithPassword("ana", PasskeySecurity.PASSWORD).status());
            ana.registerPasskey(new SoftwareAuthenticator(), "two", true, ORIGIN, RP);
            assertEquals(401, bank.browser().signInWithPassword("ana", PasskeySecurity.PASSWORD).status());
            assertEquals(200, bank.browser().signInWithPassword("ben", PasskeySecurity.PASSWORD).status());
        }
    }

    @Test
    void aWrongPasswordGivesNoSession() throws Exception {
        try (Running bank = Running.start()) {
            PasskeyBrowser b = bank.browser();
            assertEquals(401, b.signInWithPassword("ana", "wrong").status());
            assertNotEquals(200, b.me().status());
        }
    }
}
