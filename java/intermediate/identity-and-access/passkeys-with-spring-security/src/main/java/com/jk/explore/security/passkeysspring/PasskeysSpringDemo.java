package com.jk.explore.security.passkeysspring;

import java.io.IOException;
import java.util.Map;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;

/** Runs the bank's passkey story in five numbered acts against the real Spring Security WebAuthn endpoints, with an authenticator in software. */
public class PasskeysSpringDemo {

    static final String ORIGIN = PasskeySecurity.ORIGIN;
    static final String RP = PasskeySecurity.RP_ID;
    static final String LOOKALIKE = "https://bank-example.test";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys, challenges and credential ids are fresh on every run and never printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = act1Register(out, bank);
            act2SignIn(out, bank, phone);
            act3Lookalike(out, bank, phone);
        }
        act4VerificationAndCounters(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String who(PasskeyBrowser b) throws IOException, InterruptedException {
        Map<String, Object> me = b.me().json();
        return "signed in as " + me.get("name") + " with " + (me.get("signedInWith").equals("WebAuthnAuthentication") ? "a passkey" : "a password");
    }

    private static PasskeyBrowser signedInWithPassword(Running bank, String user) throws IOException, InterruptedException {
        PasskeyBrowser b = bank.browser();
        b.signInWithPassword(user, PasskeySecurity.PASSWORD);
        return b;
    }

    private static String describe(PasskeyBrowser.Attempt attempt, PasskeyBrowser browser) throws IOException, InterruptedException {
        return attempt.blocked() || attempt.status() != 200 ? attempt.text("") : "accepted (" + who(browser) + ")";
    }

    private static SoftwareAuthenticator act1Register(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. Registering a passkey: the bank keeps only a public key.");
        PasskeyBrowser ana = signedInWithPassword(bank, "ana");
        Map<String, Object> options = ana.registrationOptions();
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) options.get("user");
        @SuppressWarnings("unchecked")
        Map<String, Object> rp = (Map<String, Object>) options.get("rp");
        line(out, "  ana signs in with her password and adds a passkey: the bank, with Spring Security's WebAuthn support, sends a challenge of " + B64.decode(String.valueOf(options.get("challenge"))).length
                + " random bytes, its relying party id " + rp.get("id") + ", and her user handle of " + B64.decode(String.valueOf(user.get("id"))).length + " random bytes");
        SoftwareAuthenticator phone = new SoftwareAuthenticator();
        PasskeyBrowser.Reply registered = ana.registerPasskey(phone, "ana's phone", true, ORIGIN, RP);
        line(out, "  ana's phone makes a new P-256 key pair for " + RP + ", keeps the private key, and returns a credential id of " + B64.decode(phone.credentialId()).length
                + " bytes and an attestation object that says fmt none");
        line(out, "  the bank checks the challenge, the type, the origin and the relying party id hash: " + (registered.status() == 200 ? "accepted (passkey ana's phone registered)" : "refused " + registered.status()));
        CredentialRecord record = bank.credentials().findByCredentialId(Bytes.fromBase64(phone.credentialId()));
        line(out, "  what the bank stores for ana's passkey: a credential id of " + record.getCredentialId().getBytes().length + " bytes, a COSE public key of " + record.getPublicKey().getBytes().length
                + " bytes, a counter of " + record.getSignatureCount() + ", the label \"" + record.getLabel() + "\", and nothing that can sign");
        SoftwareAuthenticator copyOfRecords = phone.impostor();
        PasskeyBrowser thief = bank.browser();
        PasskeyBrowser.Attempt forged = thief.signInWithPasskey(copyOfRecords, true, ORIGIN, RP);
        line(out, "  a copy of the bank's records cannot sign: an answer made with another key under ana's credential id: " + forged.text("") + " (the signature does not match)");
        return phone;
    }

    private static void act2SignIn(StringBuilder out, Running bank, SoftwareAuthenticator phone) throws IOException, InterruptedException {
        line(out, "TWO. Signing in: a fresh challenge, signed once.");
        PasskeyBrowser browser = bank.browser();
        String first = String.valueOf(browser.authenticationOptions().get("challenge"));
        String second = String.valueOf(browser.authenticationOptions().get("challenge"));
        line(out, "  every sign-in gets a new challenge of " + B64.decode(first).length + " random bytes: " + !first.equals(second));
        SoftwareAuthenticator.Assertion answer = phone.sign(RP, ORIGIN, second, true, true);
        line(out, "  ana touches her phone and uses her fingerprint; it signs " + B64.decode(answer.authenticatorData()).length + " bytes of authenticator data and the SHA-256 of the client data");
        PasskeyBrowser.Reply ok = browser.postAssertion(answer);
        line(out, "  the bank checks the challenge, type, origin, relying party id hash, flags and signature: " + (ok.status() == 200 ? "accepted (" + who(browser) + ")" : "refused " + ok.status()));
        PasskeyBrowser.Reply again = bank.browser().postAssertion(answer);
        line(out, "  the same answer sent again from another browser: refused (" + again.status() + ")");
        PasskeyBrowser replayer = bank.browser();
        String used = String.valueOf(replayer.authenticationOptions().get("challenge"));
        SoftwareAuthenticator.Assertion once = phone.sign(RP, ORIGIN, used, true, true);
        replayer.postAssertion(once);
        line(out, "  the same answer sent again to the browser that asked for the challenge: refused (" + replayer.postAssertion(once).status() + ") (the challenge was used)");
        String forOther = String.valueOf(bank.browser().authenticationOptions().get("challenge"));
        line(out, "  an answer for a challenge that was given to another browser: refused (" + bank.browser().postAssertion(phone.sign(RP, ORIGIN, forOther, true, true)).status() + ")");
        PasskeyBrowser tamperer = bank.browser();
        SoftwareAuthenticator.Assertion touchOnly = phone.sign(RP, ORIGIN, String.valueOf(tamperer.authenticationOptions().get("challenge")), false, true);
        byte[] authData = B64.decode(touchOnly.authenticatorData());
        authData[32] |= 0x04;
        SoftwareAuthenticator.Assertion flipped = new SoftwareAuthenticator.Assertion(touchOnly.credentialId(), B64.encode(authData), touchOnly.clientDataJson(), touchOnly.signature(), touchOnly.userHandle());
        line(out, "  the same kind of answer with the user-verified flag switched on after signing: refused (" + tamperer.postAssertion(flipped).status() + ") (the signature does not match)");
    }

    private static void act3Lookalike(StringBuilder out, Running bank, SoftwareAuthenticator phone) throws IOException, InterruptedException {
        line(out, "THREE. A look-alike site gets nothing it can use.");
        PasskeyBrowser victim = bank.browser();
        line(out, "  ana opens " + LOOKALIKE + ", a copy of the bank's page");
        line(out, "  it asks for a passkey for its own name bank-example.test: " + victim.signInWithPasskey(phone, true, LOOKALIKE, "bank-example.test").text(""));
        line(out, "  it passes on the bank's real challenge and asks for " + RP + ": " + victim.signInWithPasskey(phone, true, LOOKALIKE, RP).text(""));
        line(out, "  even a browser that skipped that rule writes the page's real origin into the client data, and the bank checks it: refused (" + victim.signInWithPasskeyWithoutBrowserRules(phone, true, LOOKALIKE, RP).status() + ")");
        PasskeyBrowser phished = bank.browser();
        line(out, "  the same page asking for ana's password, which she types in: the password works at the bank: " + (phished.signInWithPassword("ana", PasskeySecurity.PASSWORD).status() == 200 ? "accepted (" + who(phished) + ")" : "refused")
                + "  <- A PASSWORD CAN BE PHISHED, A PASSKEY CANNOT");
    }

    private static void act4VerificationAndCounters(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. User verification, and counters against copies.");
        try (Running bank = Running.start()) {
            line(out, "  Spring Security's defaults:");
            SoftwareAuthenticator phone = registeredPhone(bank, "ana");
            PasskeyBrowser b1 = bank.browser();
            line(out, "    a sign-in with only a touch, no fingerprint: " + describe(b1.signInWithPasskey(phone, false, ORIGIN, RP), b1) + "  <- USER VERIFICATION IS ASKED FOR, NOT REQUIRED");
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            phone.setCounter(0);
            PasskeyBrowser b2 = bank.browser();
            line(out, "    the phone has signed in 3 times; restored from a backup, its counter goes back to 1: " + describe(b2.signInWithPasskey(phone, true, ORIGIN, RP), b2));
            SoftwareAuthenticator clone = phone.cloneIt();
            PasskeyBrowser b3 = bank.browser();
            line(out, "    an attacker's copy of the key, signing with the same counter as the real phone: " + describe(b3.signInWithPasskey(clone, true, ORIGIN, RP), b3) + "  <- A CLONE IS NOT NOTICED");
        }
        try (Running bank = Running.start("bank.mode=hardened")) {
            HardenedRelyingParty rp = (HardenedRelyingParty) bank.context().getBean(org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations.class);
            line(out, "  the same bank with user verification required and the counter compared (" + "HardenedRelyingParty" + " around Spring Security's relying party):");
            SoftwareAuthenticator phone = registeredPhone(bank, "ana");
            PasskeyBrowser touch = bank.browser();
            PasskeyBrowser.Attempt refused = touch.signInWithPasskey(phone, false, ORIGIN, RP);
            line(out, "    a sign-in with only a touch: refused (" + rp.refusals().getLast() + ")");
            PasskeyBrowser finger = bank.browser();
            line(out, "    the same with her fingerprint as well: " + describe(finger.signInWithPasskey(phone, true, ORIGIN, RP), finger));
            for (int i = 0; i < 4; i++) {
                bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            }
            CredentialRecord record = bank.credentials().findByCredentialId(Bytes.fromBase64(phone.credentialId()));
            line(out, "    ana's phone counts its signatures; the bank has now seen counter " + record.getSignatureCount());
            SoftwareAuthenticator clone = phone.cloneIt();
            PasskeyBrowser real = bank.browser();
            line(out, "    an attacker copies the phone's key; the real phone signs in first: " + describe(real.signInWithPasskey(phone, true, ORIGIN, RP), real));
            PasskeyBrowser copy = bank.browser();
            PasskeyBrowser.Attempt cloneAttempt = copy.signInWithPasskey(clone, true, ORIGIN, RP);
            line(out, "    then the copy signs in: refused (" + rp.refusals().getLast() + ")");
            PasskeyBrowser again = bank.browser();
            PasskeyBrowser.Attempt realAgain = again.signInWithPasskey(phone, true, ORIGIN, RP);
            line(out, "    the passkey is now suspended until ana confirms it, even for the real phone: refused (" + rp.refusals().getLast() + ")");
            SoftwareAuthenticator synced = new SoftwareAuthenticator().synced();
            PasskeyBrowser ben = signedInWithPassword(bank, "ben");
            ben.registerPasskey(synced, "ben's synced passkey", true, ORIGIN, RP);
            PasskeyBrowser s1 = bank.browser();
            PasskeyBrowser s2 = bank.browser();
            line(out, "    a synced passkey, counter 0: " + describe(s1.signInWithPasskey(synced, true, ORIGIN, RP), s1));
            line(out, "    a synced passkey, counter 0: " + describe(s2.signInWithPasskey(synced, true, ORIGIN, RP), s2));
            line(out, "    a synced passkey always reports 0, so a copy cannot be spotted by its counter: its protection is the account it syncs through");
            if (refused.status() == 200 || cloneAttempt.status() == 200 || realAgain.status() == 200) {
                line(out, "    (unexpected: a refusal was accepted)");
            }
        }
    }

    private static SoftwareAuthenticator registeredPhone(Running bank, String user) throws IOException, InterruptedException {
        PasskeyBrowser b = signedInWithPassword(bank, user);
        SoftwareAuthenticator phone = new SoftwareAuthenticator();
        b.registerPasskey(phone, user + "'s phone", true, ORIGIN, RP);
        return phone;
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill: recovery, lost devices and the move away from passwords.");
        try (Running bank = Running.start()) {
            PasskeyBrowser ben = signedInWithPassword(bank, "ben");
            SoftwareAuthenticator phone = new SoftwareAuthenticator();
            SoftwareAuthenticator key = new SoftwareAuthenticator();
            ben.registerPasskey(phone, "ben's phone", true, ORIGIN, RP);
            ben.registerPasskey(key, "ben's security key", true, ORIGIN, RP);
            int registered = bank.credentials().findByUserId(bank.users().findByUsername("ben").getId()).size();
            line(out, "  ben registers " + registered + " passkeys, his phone and a security key, and the bank keeps for each a public key and a label");
            PasskeyBrowser laptop = bank.browser();
            line(out, "  ben's phone is lost; his new laptop has no passkey yet: " + laptop.signInWithPasskey(SoftwareAuthenticator.empty(), true, ORIGIN, RP).text(""));
            PasskeyBrowser drawer = bank.browser();
            line(out, "  the security key from his drawer: " + describe(drawer.signInWithPasskey(key, true, ORIGIN, RP), drawer));
            PasskeyBrowser.Reply removed = drawer.send("DELETE", "/webauthn/register/" + phone.credentialId(), null, null);
            line(out, "  signed in with the key, ben removes the lost phone's passkey: " + removed.status() + ", and a thief who holds the phone and its fingerprint: "
                    + bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).text("") + "");
            line(out, "  recovery is now the weakest way in, so it needs the same care as the passkeys themselves");
            PasskeyBrowser ana = signedInWithPassword(bank, "ana");
            line(out, "  ana's password still works alongside her passkeys: " + (ana.me().status() == 200 ? "accepted (" + who(ana) + ")" : "refused") + "  <- THE WEAKEST WAY IN SETS THE LEVEL");
        }
        try (Running bank = Running.start()) {
            PasskeyBrowser ana = signedInWithPassword(bank, "ana");
            ana.registerPasskey(new SoftwareAuthenticator(), "ana's phone", true, ORIGIN, RP);
            ana.registerPasskey(new SoftwareAuthenticator(), "ana's security key", true, ORIGIN, RP);
            PasskeyBrowser later = bank.browser();
            line(out, "  once ana has two passkeys and the bank switches her password off: refused (" + later.signInWithPassword("ana", PasskeySecurity.PASSWORD).status() + ")");
            PasskeyBrowser ben = bank.browser();
            line(out, "  a customer with no passkey keeps the password: " + (ben.signInWithPassword("ben", PasskeySecurity.PASSWORD).status() == 200 ? "accepted (" + who(ben) + ")" : "refused"));
        }
        line(out, "  a real bank uses a maintained WebAuthn library, as here: Spring Security's WebAuthn support, built on WebAuthn4J");
    }
}
