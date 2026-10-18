package com.jk.explore.security.passkeys;

import java.util.List;

/**
 * Runs the online bank's passwordless sign-in in five numbered acts. Keys, challenges and credential ids are random, so the demo prints
 * only facts that do not depend on them: sizes, flags, counters and verdicts.
 */
public class PasskeysDemo {

    static final String RP_ID = "bank.example";
    static final String ORIGIN = "https://bank.example";
    static final String PHISHING_ORIGIN = "https://bank-example.test";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        SimClock clock = new SimClock();
        RelyingParty bank = new RelyingParty(RP_ID, ORIGIN, clock);
        Authenticator phone = new Authenticator("ana's phone", false);
        Browser browser = new Browser(phone);
        bank.setPassword("ana", "Tulip-2026".toCharArray());
        act1Registration(out, bank, browser);
        act2SignIn(out, bank, browser, phone, clock);
        act3Phishing(out, bank, browser, phone);
        act4VerificationAndCounters(out, bank, browser, phone);
        act5Bill(out, bank, clock);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Registration(StringBuilder out, RelyingParty bank, Browser browser) {
        line(out, "ONE. Registering a passkey: the bank keeps only a public key.");
        RelyingParty.Options options = bank.startRegistration("ana");
        line(out, "  the bank sends a challenge of " + B64.decode(options.challenge()).length + " random bytes and its relying party id " + options.rpId());
        Authenticator.Registration registration = browser.create(ORIGIN, options.rpId(), options.challenge(), true).registration().orElseThrow();
        line(out, "  ana's phone makes a new P-256 key pair for " + RP_ID + ", keeps the private key, and returns a credential id of "
                + registration.credentialId().length + " bytes and a public key of " + registration.publicKey().length + " bytes");
        line(out, "  the bank checks the challenge, the type, the origin and the relying party id hash: "
                + bank.finishRegistration("ana", registration, "ana's phone").text());
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        line(out, "  what the bank stores for ana's passkey: a credential id, a " + stored.publicKey.length
                + "-byte public key and a counter of " + stored.signCount + ", and nothing that can sign");
        Authenticator other = new Authenticator("someone else's laptop", false);
        other.create(RP_ID, new ClientData(ClientData.CREATE, "unused", ORIGIN).json(), true);
        RelyingParty.Options signIn = bank.startSignIn("ana", false);
        byte[] clientData = new ClientData(ClientData.GET, signIn.challenge(), ORIGIN).json();
        Authenticator.Assertion forged = other.get(RP_ID, List.of(), clientData, true).orElseThrow().withCredentialId(stored.id);
        line(out, "  a copy of the bank's records cannot sign: an answer made with another key under ana's credential id: "
                + bank.finishSignIn("ana", forged).text());
    }

    private static void act2SignIn(StringBuilder out, RelyingParty bank, Browser browser, Authenticator phone, SimClock clock) {
        line(out, "TWO. Signing in: a fresh challenge, signed once.");
        RelyingParty.Options options = bank.startSignIn("ana", false);
        Authenticator.Assertion answer = browser.get(ORIGIN, RP_ID, options.challenge(), options.allowed(), false).assertion().orElseThrow();
        line(out, "  ana touches her phone; it signs " + answer.authenticatorData().length
                + " bytes of authenticator data and the SHA-256 of the client data");
        line(out, "  the bank checks challenge, type, origin, relying party id hash, flags, signature and counter: "
                + bank.finishSignIn("ana", answer).text());
        line(out, "  the same answer sent again: " + bank.finishSignIn("ana", answer).text());
        RelyingParty.Options late = bank.startSignIn("ana", false);
        Authenticator.Assertion slow = browser.get(ORIGIN, RP_ID, late.challenge(), late.allowed(), false).assertion().orElseThrow();
        clock.advance(RelyingParty.CHALLENGE_SECONDS + 1);
        line(out, "  an answer that arrives " + (RelyingParty.CHALLENGE_SECONDS + 1) + " seconds after its challenge: " + bank.finishSignIn("ana", slow).text());
        RelyingParty.Options again = bank.startSignIn("ana", false);
        Authenticator.Assertion touched = browser.get(ORIGIN, RP_ID, again.challenge(), again.allowed(), false).assertion().orElseThrow();
        byte[] data = touched.authenticatorData().clone();
        data[32] |= AuthenticatorData.USER_VERIFIED;
        Authenticator.Assertion edited = new Authenticator.Assertion(touched.credentialId(), data, touched.clientDataJson(), touched.signature());
        line(out, "  the same kind of answer with the user-verified flag switched on after signing: " + bank.finishSignIn("ana", edited).text());
    }

    private static void act3Phishing(StringBuilder out, RelyingParty bank, Browser browser, Authenticator phone) {
        line(out, "THREE. A look-alike site gets nothing it can use.");
        line(out, "  ana opens " + PHISHING_ORIGIN + ", a copy of the bank's page");
        line(out, "  it asks for a passkey for its own name bank-example.test: "
                + refusal(browser.get(PHISHING_ORIGIN, "bank-example.test", "c2l0ZQ", List.of(), false)));
        RelyingParty.Options real = bank.startSignIn("ana", false);
        line(out, "  it passes on the bank's real challenge and asks for " + RP_ID + ": "
                + refusal(browser.get(PHISHING_ORIGIN, RP_ID, real.challenge(), real.allowed(), false)));
        byte[] honestClientData = new ClientData(ClientData.GET, real.challenge(), PHISHING_ORIGIN).json();
        Authenticator.Assertion relayed = phone.get(RP_ID, real.allowed(), honestClientData, false).orElseThrow();
        line(out, "  even a browser that skipped that rule writes the page's real origin, and the bank checks it: "
                + bank.finishSignIn("ana", relayed).text());
        line(out, "  the same page asking for ana's password, which she types in: the password works at the bank: "
                + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text() + "  <- A PASSWORD CAN BE PHISHED, A PASSKEY CANNOT");
    }

    private static String refusal(Browser.Result result) {
        return result.ok() ? "an answer came back" : "nothing (" + result.refusal() + ")";
    }

    private static void act4VerificationAndCounters(StringBuilder out, RelyingParty bank, Browser browser, Authenticator phone) {
        line(out, "FOUR. User verification for payments, and counters against copies.");
        RelyingParty.Options pay = bank.startSignIn("ana", true);
        line(out, "  a payment of 900 with only a touch: "
                + bank.finishSignIn("ana", browser.get(ORIGIN, RP_ID, pay.challenge(), pay.allowed(), false).assertion().orElseThrow()).text());
        RelyingParty.Options payAgain = bank.startSignIn("ana", true);
        line(out, "  the same payment with her fingerprint as well: "
                + bank.finishSignIn("ana", browser.get(ORIGIN, RP_ID, payAgain.challenge(), payAgain.allowed(), true).assertion().orElseThrow()).text());
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        line(out, "  ana's phone counts its signatures; the bank has now seen counter " + stored.signCount);
        Authenticator copy = phone.copyAs("copied key");
        RelyingParty.Options first = bank.startSignIn("ana", false);
        line(out, "  an attacker copies the phone's key; the real phone signs in first: "
                + bank.finishSignIn("ana", browser.get(ORIGIN, RP_ID, first.challenge(), first.allowed(), false).assertion().orElseThrow()).text());
        RelyingParty.Options second = bank.startSignIn("ana", false);
        line(out, "  then the copy signs in: "
                + bank.finishSignIn("ana", new Browser(copy).get(ORIGIN, RP_ID, second.challenge(), second.allowed(), false).assertion().orElseThrow()).text());
        RelyingParty.Options third = bank.startSignIn("ana", false);
        line(out, "  the passkey is now suspended until ana confirms it, even for the real phone: "
                + bank.finishSignIn("ana", browser.get(ORIGIN, RP_ID, third.challenge(), third.allowed(), false).assertion().orElseThrow()).text());
        Authenticator laptop = new Authenticator("ana's synced passkey", true);
        Browser laptopBrowser = new Browser(laptop);
        RelyingParty.Options register = bank.startRegistration("ana");
        bank.finishRegistration("ana", laptopBrowser.create(ORIGIN, RP_ID, register.challenge(), true).registration().orElseThrow(), "ana's synced passkey");
        for (int i = 0; i < 2; i++) {
            RelyingParty.Options synced = bank.startSignIn("ana", false);
            Authenticator.Assertion answer = laptopBrowser.get(ORIGIN, RP_ID, synced.challenge(), synced.allowed(), false).assertion().orElseThrow();
            line(out, "  a synced passkey, counter " + AuthenticatorData.decode(answer.authenticatorData()).signCount() + ": "
                    + bank.finishSignIn("ana", answer).text());
        }
        line(out, "  a synced passkey always reports 0, so a copy cannot be spotted by its counter: its protection is the account it syncs through");
    }

    private static void act5Bill(StringBuilder out, RelyingParty bank, SimClock clock) {
        line(out, "FIVE. The bill: recovery, lost devices and the move away from passwords.");
        Authenticator bensPhone = new Authenticator("ben's phone", false);
        Authenticator bensKey = new Authenticator("ben's security key", false);
        for (Authenticator device : List.of(bensPhone, bensKey)) {
            RelyingParty.Options options = bank.startRegistration("ben");
            bank.finishRegistration("ben", new Browser(device).create(ORIGIN, RP_ID, options.challenge(), true).registration().orElseThrow(), device.name());
        }
        String code = bank.issueRecoveryCode("ben");
        line(out, "  ben registers 2 passkeys, his phone and a security key, and keeps 1 recovery code of " + code.length() + " characters");
        Authenticator newLaptop = new Authenticator("ben's new laptop", false);
        RelyingParty.Options lost = bank.startSignIn("ben", false);
        line(out, "  ben's phone is lost; his new laptop has no passkey yet: "
                + refusal(new Browser(newLaptop).get(ORIGIN, RP_ID, lost.challenge(), lost.allowed(), false)));
        RelyingParty.Options withKey = bank.startSignIn("ben", false);
        line(out, "  the security key from his drawer: "
                + bank.finishSignIn("ben", new Browser(bensKey).get(ORIGIN, RP_ID, withKey.challenge(), withKey.allowed(), false).assertion().orElseThrow()).text());
        line(out, "  the recovery code, once: " + bank.recover("ben", code).text());
        line(out, "  the recovery code, twice: " + bank.recover("ben", code).text());
        line(out, "  recovery is now the weakest way in, so it needs the same care as the passkeys themselves");
        line(out, "  ana's password still works alongside her passkeys: " + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text()
                + "  <- THE WEAKEST WAY IN SETS THE LEVEL");
        bank.disablePassword("ana");
        line(out, "  once ana has two passkeys and the bank switches her password off: " + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text());
        line(out, "  a real bank uses a maintained WebAuthn library: the -with-spring-security version uses Spring Security's passkey support");
    }
}
