package com.jk.explore.security.passkeys;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/**
 * Runs the online bank's passwordless sign-in in five numbered acts, each split into labelled sections. Every random value comes from
 * one FixedRandom, so the challenges, keys and signatures it prints are the same on every run.
 */
public class PasskeysDemo {

    static final String RP_ID = "bank.example";
    static final String ORIGIN = "https://bank.example";
    static final String PHISHING_ORIGIN = "https://bank-example.test";
    static final String PHISHING_HOST = "bank-example.test";
    static final long SEED = 2026;

    private static final HexFormat HEX = HexFormat.of();

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        FixedRandom random = new FixedRandom(SEED);
        SimClock clock = new SimClock();
        RelyingParty bank = new RelyingParty(RP_ID, ORIGIN, clock, random);
        Authenticator phone = new Authenticator("ana's phone", false, random);
        Browser browser = new Browser(phone);
        bank.setPassword("ana", "Tulip-2026".toCharArray());
        act1Registration(out, bank, browser, random);
        act2SignIn(out, bank, browser, clock);
        act3Phishing(out, bank, browser, phone);
        act4VerificationAndCounters(out, bank, browser, phone, random);
        act5Bill(out, bank, random);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Client data JSON on three short lines, split before "challenge" and "origin", exactly as signed (no spaces added). */
    private static void clientData(StringBuilder out, byte[] json) {
        String text = new String(json, StandardCharsets.UTF_8);
        int c = text.indexOf(",\"challenge\"");
        int o = text.indexOf(",\"origin\"");
        line(out, "    " + text.substring(0, c + 1));
        line(out, "    " + text.substring(c + 1, o + 1));
        line(out, "    " + text.substring(o + 1));
    }

    private static String hex(byte[] bytes, int from, int to) {
        return HEX.formatHex(bytes, from, to);
    }

    private static void act1Registration(StringBuilder out, RelyingParty bank, Browser browser, FixedRandom random) {
        line(out, "ONE. Registering a passkey: the bank keeps only a public key.");
        RelyingParty.Options options = bank.startRegistration("ana");
        line(out, "  THE REQUEST (the bank to ana's phone, through the browser):");
        line(out, "    challenge  " + options.challenge());
        line(out, "               " + B64.decode(options.challenge()).length + " random bytes, base64url");
        line(out, "    rp id      " + options.rpId());
        Authenticator.Registration registration = browser.create(ORIGIN, options.rpId(), options.challenge(), true).registration().orElseThrow();
        line(out, "  THE CLIENT DATA (the browser writes it, not the page):");
        clientData(out, registration.clientDataJson());
        line(out, "  THE RESPONSE (a new P-256 key pair, made for " + RP_ID + "):");
        line(out, "    credential id  " + HEX.formatHex(registration.credentialId()) + " (" + registration.credentialId().length + " bytes)");
        line(out, "    public key     " + registration.publicKey().length + " bytes, starts " + hex(registration.publicKey(), 0, 8));
        line(out, "    private key    stays on ana's phone");
        line(out, "  THE BANK'S CHECKS (challenge, type, origin, rp id hash):");
        line(out, "    " + bank.finishRegistration("ana", registration, "ana's phone").text());
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        line(out, "  WHAT THE BANK STORES:");
        line(out, "    credential id " + stored.id.length + " bytes, public key " + stored.publicKey.length + " bytes, counter " + stored.signCount);
        line(out, "    nothing that can sign");
        Authenticator other = new Authenticator("someone else's laptop", false, random);
        other.create(RP_ID, new ClientData(ClientData.CREATE, "unused", ORIGIN).json(), true);
        RelyingParty.Options signIn = bank.startSignIn("ana", false);
        byte[] clientData = new ClientData(ClientData.GET, signIn.challenge(), ORIGIN).json();
        Authenticator.Assertion forged = other.get(RP_ID, List.of(), clientData, true).orElseThrow().withCredentialId(stored.id);
        line(out, "  A COPY OF THE RECORDS (another key, ana's credential id):");
        line(out, "    " + bank.finishSignIn("ana", forged).text());
    }

    private static void act2SignIn(StringBuilder out, RelyingParty bank, Browser browser, SimClock clock) {
        line(out, "TWO. Signing in: a fresh challenge, signed once.");
        RelyingParty.Options options = bank.startSignIn("ana", false);
        line(out, "  THE REQUEST (a fresh challenge, " + B64.decode(options.challenge()).length + " bytes):");
        line(out, "    " + options.challenge());
        Authenticator.Assertion answer = browser.get(ORIGIN, RP_ID, options.challenge(), options.allowed(), false).assertion().orElseThrow();
        byte[] json = answer.clientDataJson();
        byte[] data = answer.authenticatorData();
        line(out, "  STEP 1. THE CLIENT DATA (" + json.length + " bytes, the browser writes it):");
        clientData(out, json);
        byte[] clientHash = Sha256.of(json);
        line(out, "  STEP 2. SHA-256 OF THE CLIENT DATA (32 bytes):");
        line(out, "    " + hex(clientHash, 0, 16));
        line(out, "    " + hex(clientHash, 16, 32));
        AuthenticatorData decoded = AuthenticatorData.decode(data);
        line(out, "  STEP 3. THE AUTHENTICATOR DATA (" + data.length + " bytes, the phone writes it):");
        line(out, "    rp id hash  " + hex(data, 0, 8) + "...  SHA-256 of " + RP_ID);
        line(out, "    flags       " + hex(data, 32, 33) + "  user present, not verified");
        line(out, "    counter     " + hex(data, 33, 37) + "  = " + decoded.signCount());
        byte[] signed = Authenticator.signedBytes(data, json);
        line(out, "  STEP 4. THE SIGNED BYTES (authenticator data + hash):");
        line(out, "    " + data.length + " + " + clientHash.length + " = " + signed.length + " bytes");
        line(out, "  STEP 5. THE SIGNATURE (ES256, with the private key):");
        line(out, "    " + answer.signature().length + " bytes, starts " + hex(answer.signature(), 0, 12));
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        line(out, "  THE BANK'S CHECK (the stored public key, the same 69 bytes):");
        line(out, "    signature valid: " + RelyingParty.verify(stored.publicKey, answer));
        line(out, "    " + bank.finishSignIn("ana", answer).text());
        line(out, "  THE SAME ANSWER SENT AGAIN:");
        line(out, "    " + bank.finishSignIn("ana", answer).text());
        RelyingParty.Options late = bank.startSignIn("ana", false);
        Authenticator.Assertion slow = browser.get(ORIGIN, RP_ID, late.challenge(), late.allowed(), false).assertion().orElseThrow();
        clock.advance(RelyingParty.CHALLENGE_SECONDS + 1);
        line(out, "  AN ANSWER " + (RelyingParty.CHALLENGE_SECONDS + 1) + " SECONDS AFTER ITS CHALLENGE (limit "
                + RelyingParty.CHALLENGE_SECONDS + "):");
        line(out, "    " + bank.finishSignIn("ana", slow).text());
        RelyingParty.Options again = bank.startSignIn("ana", false);
        Authenticator.Assertion touched = browser.get(ORIGIN, RP_ID, again.challenge(), again.allowed(), false).assertion().orElseThrow();
        byte[] edited = touched.authenticatorData().clone();
        edited[32] |= AuthenticatorData.USER_VERIFIED;
        line(out, "  FLAGS CHANGED AFTER SIGNING (" + hex(touched.authenticatorData(), 32, 33) + " -> " + hex(edited, 32, 33) + "):");
        Authenticator.Assertion forged = new Authenticator.Assertion(touched.credentialId(), edited, touched.clientDataJson(), touched.signature());
        line(out, "    " + bank.finishSignIn("ana", forged).text());
    }

    private static void act3Phishing(StringBuilder out, RelyingParty bank, Browser browser, Authenticator phone) {
        line(out, "THREE. A look-alike site gets nothing it can use.");
        line(out, "  THE PAGE (a copy of the bank's page):");
        line(out, "    " + PHISHING_ORIGIN);
        line(out, "  REQUEST 1. A PASSKEY FOR ITS OWN NAME " + PHISHING_HOST + ":");
        line(out, "    " + refusal(browser.get(PHISHING_ORIGIN, PHISHING_HOST, "c2l0ZQ", List.of(), false)));
        RelyingParty.Options real = bank.startSignIn("ana", false);
        line(out, "  REQUEST 2. THE BANK'S REAL CHALLENGE, RP ID " + RP_ID + ":");
        line(out, "    is " + PHISHING_HOST + " equal to " + RP_ID + "?  " + yesNo(PHISHING_HOST.equals(RP_ID)));
        line(out, "    does it end with ." + RP_ID + "?  " + yesNo(PHISHING_HOST.endsWith("." + RP_ID)));
        line(out, "    " + refusal(browser.get(PHISHING_ORIGIN, RP_ID, real.challenge(), real.allowed(), false)));
        byte[] honestClientData = new ClientData(ClientData.GET, real.challenge(), PHISHING_ORIGIN).json();
        Authenticator.Assertion relayed = phone.get(RP_ID, real.allowed(), honestClientData, false).orElseThrow();
        line(out, "  REQUEST 3. A BROWSER THAT SKIPPED THAT RULE:");
        String text = new String(honestClientData, StandardCharsets.UTF_8);
        line(out, "    signed client data: " + text.substring(text.indexOf("\"origin\""), text.indexOf(",\"crossOrigin\"")));
        line(out, "    the bank expects:   \"origin\":\"" + ORIGIN + "\"");
        line(out, "    " + bank.finishSignIn("ana", relayed).text());
        line(out, "  THE SAME PAGE ASKS FOR ANA'S PASSWORD, AND SHE TYPES IT:");
        line(out, "    replayed at the bank: " + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text());
        line(out, "  a password can be phished; a passkey cannot");
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String refusal(Browser.Result result) {
        return result.ok() ? "an answer came back" : "nothing (" + result.refusal() + ")";
    }

    private static Authenticator.Assertion answer(Browser browser, RelyingParty.Options options, boolean verified) {
        return browser.get(ORIGIN, RP_ID, options.challenge(), options.allowed(), verified).assertion().orElseThrow();
    }

    private static long counter(Authenticator.Assertion answer) {
        return AuthenticatorData.decode(answer.authenticatorData()).signCount();
    }

    private static void act4VerificationAndCounters(StringBuilder out, RelyingParty bank, Browser browser, Authenticator phone,
            FixedRandom random) {
        line(out, "FOUR. User verification for payments, and counters against copies.");
        line(out, "  A PAYMENT OF 900 (the bank asks for user verification):");
        Authenticator.Assertion touch = answer(browser, bank.startSignIn("ana", true), false);
        line(out, "    touch only, flags " + hex(touch.authenticatorData(), 32, 33) + ":");
        line(out, "      " + bank.finishSignIn("ana", touch).text());
        Authenticator.Assertion finger = answer(browser, bank.startSignIn("ana", true), true);
        line(out, "    touch and fingerprint, flags " + hex(finger.authenticatorData(), 32, 33) + ":");
        line(out, "      " + bank.finishSignIn("ana", finger).text());
        RelyingParty.StoredCredential stored = bank.credentialsOf("ana").getFirst();
        line(out, "  THE COUNTERS (the bank keeps the last one it saw):");
        line(out, "    the bank has seen " + stored.signCount);
        Authenticator copy = phone.copyAs("copied key");
        Authenticator.Assertion first = answer(browser, bank.startSignIn("ana", false), false);
        long seen = stored.signCount;
        line(out, "    real phone:  counter " + counter(first) + ", " + counter(first) + " > " + seen + "?  " + yesNo(counter(first) > seen));
        line(out, "      " + bank.finishSignIn("ana", first).text());
        Authenticator.Assertion second = answer(new Browser(copy), bank.startSignIn("ana", false), false);
        seen = stored.signCount;
        line(out, "    copied key:  counter " + counter(second) + ", " + counter(second) + " > " + seen + "?  " + yesNo(counter(second) > seen));
        line(out, "      " + bank.finishSignIn("ana", second).text());
        Authenticator.Assertion third = answer(browser, bank.startSignIn("ana", false), false);
        line(out, "    real phone again, counter " + counter(third) + ":");
        line(out, "      " + bank.finishSignIn("ana", third).text());
        Authenticator laptop = new Authenticator("ana's synced passkey", true, random);
        Browser laptopBrowser = new Browser(laptop);
        RelyingParty.Options register = bank.startRegistration("ana");
        bank.finishRegistration("ana", laptopBrowser.create(ORIGIN, RP_ID, register.challenge(), true).registration().orElseThrow(), "ana's synced passkey");
        line(out, "  A SYNCED PASSKEY (shared between ana's devices):");
        for (int i = 0; i < 2; i++) {
            Authenticator.Assertion synced = answer(laptopBrowser, bank.startSignIn("ana", false), false);
            line(out, "    counter " + counter(synced) + ": " + bank.finishSignIn("ana", synced).text());
        }
        line(out, "  a synced passkey always reports 0: a copy cannot be spotted");
    }

    private static void act5Bill(StringBuilder out, RelyingParty bank, FixedRandom random) {
        line(out, "FIVE. The bill: recovery, lost devices and the move away from passwords.");
        Authenticator bensPhone = new Authenticator("ben's phone", false, random);
        Authenticator bensKey = new Authenticator("ben's security key", false, random);
        for (Authenticator device : List.of(bensPhone, bensKey)) {
            RelyingParty.Options options = bank.startRegistration("ben");
            bank.finishRegistration("ben", new Browser(device).create(ORIGIN, RP_ID, options.challenge(), true).registration().orElseThrow(), device.name());
        }
        String code = bank.issueRecoveryCode("ben");
        line(out, "  BEN'S WAYS IN:");
        line(out, "    " + bank.credentialsOf("ben").size() + " passkeys: his phone and a security key");
        line(out, "    1 recovery code of " + code.length() + " characters; the bank keeps its SHA-256");
        line(out, "  BEN'S PHONE IS LOST:");
        Authenticator newLaptop = new Authenticator("ben's new laptop", false, random);
        RelyingParty.Options lost = bank.startSignIn("ben", false);
        line(out, "    new laptop:");
        line(out, "      " + refusal(new Browser(newLaptop).get(ORIGIN, RP_ID, lost.challenge(), lost.allowed(), false)));
        RelyingParty.Options withKey = bank.startSignIn("ben", false);
        line(out, "    security key:");
        line(out, "      " + bank.finishSignIn("ben",
                new Browser(bensKey).get(ORIGIN, RP_ID, withKey.challenge(), withKey.allowed(), false).assertion().orElseThrow()).text());
        line(out, "    recovery code, once:");
        line(out, "      " + bank.recover("ben", code).text());
        line(out, "    recovery code, twice:");
        line(out, "      " + bank.recover("ben", code).text());
        line(out, "  recovery is now the weakest way in: guard it like a passkey");
        line(out, "  ANA'S OLD PASSWORD, STILL SWITCHED ON:");
        line(out, "    " + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text() + "  <- THE WEAKEST WAY IN");
        bank.disablePassword("ana");
        line(out, "  ANA'S PASSWORD, SWITCHED OFF (she has two passkeys):");
        line(out, "    " + bank.passwordSignIn("ana", "Tulip-2026".toCharArray()).text());
        line(out, "  a real bank uses a maintained WebAuthn library");
    }
}
