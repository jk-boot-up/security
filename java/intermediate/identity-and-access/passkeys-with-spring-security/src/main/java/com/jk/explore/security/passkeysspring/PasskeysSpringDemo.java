package com.jk.explore.security.passkeysspring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;

/**
 * Runs the bank's passkey story in five numbered acts against the real Spring Security WebAuthn endpoints, with an authenticator in software. Challenges, user
 * handles, keys and signatures are fresh on every run, so the output shows them as placeholders and prints only what does not change: sizes, the fields of the
 * options, the relying party id hash, flags, counters and HTTP statuses.
 */
public class PasskeysSpringDemo {

    static final String ORIGIN = PasskeySecurity.ORIGIN;
    static final String RP = PasskeySecurity.RP_ID;
    static final String LOOKALIKE = "https://bank-example.test";
    static final String LOOKALIKE_HOST = "bank-example.test";

    private static final HexFormat HEX = HexFormat.of();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    /** A random value is printed as its size only, because it changes on every run. */
    private static String random(Object base64url) {
        return "<" + B64.decode(String.valueOf(base64url)).length + " random bytes>";
    }

    private static String flags(SoftwareAuthenticator.Assertion a) {
        return HEX.formatHex(B64.decode(a.authenticatorData()), 32, 33);
    }

    private static long counter(SoftwareAuthenticator.Assertion a) {
        byte[] data = B64.decode(a.authenticatorData());
        return Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(data, 33, 4).getInt());
    }

    private static SoftwareAuthenticator act1Register(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. Registering a passkey: the bank keeps only a public key.");
        PasskeyBrowser ana = signedInWithPassword(bank, "ana");
        Map<String, Object> options = ana.registrationOptions();
        Map<String, Object> rp = map(options.get("rp"));
        Map<String, Object> selection = map(options.get("authenticatorSelection"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) options.get("pubKeyCredParams");
        line(out, "  THE REQUEST (POST /webauthn/register/options, as ana):");
        line(out, "    challenge          " + random(options.get("challenge")));
        line(out, "    rp                 id " + rp.get("id") + ", name " + rp.get("name"));
        line(out, "    user handle        " + random(map(options.get("user")).get("id")));
        line(out, "    algorithms         " + params.stream().map(p -> String.valueOf(p.get("alg"))).toList());
        line(out, "                       (-8 EdDSA, -7 ES256, -257 RS256)");
        line(out, "    user verification  " + selection.get("userVerification"));
        line(out, "    attestation        " + options.get("attestation"));
        SoftwareAuthenticator phone = new SoftwareAuthenticator();
        PasskeyBrowser.Reply registered = ana.registerPasskey(phone, "ana's phone", true, ORIGIN, RP);
        CredentialRecord record = bank.credentials().findByCredentialId(Bytes.fromBase64(phone.credentialId()));
        byte[] cose = record.getPublicKey().getBytes();
        line(out, "  THE RESPONSE (POST /webauthn/register, a new P-256 key pair):");
        line(out, "    credential id      " + B64.decode(phone.credentialId()).length + " bytes");
        line(out, "    attestation        fmt none: no statement about the phone's make");
        line(out, "    COSE public key    " + cose.length + " bytes, starts " + HEX.formatHex(cose, 0, 10));
        line(out, "  THE COSE KEY, DECODED (CBOR: label = value):");
        for (Object[] pair : Cbor.readMap(cose)) {
            String value = pair[1] instanceof byte[] b ? b.length + " bytes" : String.valueOf(pair[1]);
            line(out, "    " + String.format("%-3s", pair[0]) + " " + String.format("%-8s", coseLabel((Integer) pair[0])) + " = " + value + coseMeaning((Integer) pair[0], pair[1]));
        }
        line(out, "  THE BANK'S CHECKS (challenge, type, origin, rp id hash):");
        line(out, "    " + registered.status() + " " + (registered.status() == 200 ? "accepted (passkey ana's phone registered)" : "refused"));
        line(out, "  WHAT THE BANK STORES (a CredentialRecord):");
        line(out, "    credential id " + record.getCredentialId().getBytes().length + " bytes, public key " + cose.length + " bytes, counter "
                + record.getSignatureCount());
        line(out, "    label \"" + record.getLabel() + "\", and nothing that can sign");
        SoftwareAuthenticator copyOfRecords = phone.impostor();
        PasskeyBrowser.Attempt forged = bank.browser().signInWithPasskey(copyOfRecords, true, ORIGIN, RP);
        line(out, "  A COPY OF THE RECORDS (another key, ana's credential id):");
        line(out, "    " + forged.text("") + ": the signature does not match");
        return phone;
    }

    private static String coseLabel(int label) {
        return switch (label) {
            case 1 -> "kty";
            case 3 -> "alg";
            case -1 -> "crv";
            case -2 -> "x";
            case -3 -> "y";
            default -> "?";
        };
    }

    private static String coseMeaning(int label, Object value) {
        return switch (label) {
            case 1 -> value.equals(2) ? "  (EC2: an elliptic-curve key)" : "";
            case 3 -> value.equals(-7) ? "  (ES256)" : "";
            case -1 -> value.equals(1) ? "  (P-256)" : "";
            case -2, -3 -> "  (a point on the curve)";
            default -> "";
        };
    }

    private static void act2SignIn(StringBuilder out, Running bank, SoftwareAuthenticator phone) throws IOException, InterruptedException {
        line(out, "TWO. Signing in: a fresh challenge, signed once.");
        PasskeyBrowser browser = bank.browser();
        String first = String.valueOf(browser.authenticationOptions().get("challenge"));
        Map<String, Object> options = browser.authenticationOptions();
        String second = String.valueOf(options.get("challenge"));
        line(out, "  THE REQUEST (POST /webauthn/authenticate/options):");
        line(out, "    challenge          " + random(second));
        line(out, "    new on every call  " + !first.equals(second));
        line(out, "    rp id              " + options.get("rpId"));
        line(out, "    user verification  " + options.get("userVerification"));
        SoftwareAuthenticator.Assertion answer = phone.sign(RP, ORIGIN, second, true, true);
        byte[] clientData = B64.decode(answer.clientDataJson());
        byte[] authData = B64.decode(answer.authenticatorData());
        String text = new String(clientData, StandardCharsets.UTF_8).replace(second, "<challenge>");
        line(out, "  STEP 1. THE CLIENT DATA (the browser writes it):");
        line(out, "    " + text.substring(0, text.indexOf(",\"challenge\"") + 1));
        line(out, "    " + text.substring(text.indexOf("\"challenge\""), text.indexOf(",\"origin\"") + 1));
        line(out, "    " + text.substring(text.indexOf("\"origin\"")));
        line(out, "  STEP 2. THE AUTHENTICATOR DATA (" + authData.length + " bytes, the phone writes it):");
        line(out, "    rp id hash  " + HEX.formatHex(authData, 0, 8) + "...  SHA-256 of " + RP);
        line(out, "    flags       " + flags(answer) + "  user present + user verified");
        line(out, "    counter     " + HEX.formatHex(authData, 33, 37) + "  = " + counter(answer));
        byte[] signed = SoftwareAuthenticator.signedBytes(authData, clientData);
        line(out, "  STEP 3. THE SIGNED BYTES (authenticator data + SHA-256):");
        line(out, "    " + authData.length + " + " + (signed.length - authData.length) + " = " + signed.length + " bytes");
        line(out, "  STEP 4. THE SIGNATURE (ES256, with the private key):");
        line(out, "    <about 70 bytes, different on every run>");
        PasskeyBrowser.Reply ok = browser.postAssertion(answer);
        line(out, "  THE RESPONSE (POST /login/webauthn, Spring Security checks it):");
        line(out, "    " + ok.status() + " " + (ok.status() == 200 ? "accepted (" + who(browser) + ")" : "refused"));
        line(out, "  THE SAME ANSWER, OR A CHANGED ONE:");
        line(out, "    again, from another browser:      refused (" + bank.browser().postAssertion(answer).status() + ")");
        PasskeyBrowser replayer = bank.browser();
        String used = String.valueOf(replayer.authenticationOptions().get("challenge"));
        SoftwareAuthenticator.Assertion once = phone.sign(RP, ORIGIN, used, true, true);
        replayer.postAssertion(once);
        line(out, "    again, to the browser that asked: refused (" + replayer.postAssertion(once).status() + ")");
        String forOther = String.valueOf(bank.browser().authenticationOptions().get("challenge"));
        line(out, "    another browser's challenge:      refused ("
                + bank.browser().postAssertion(phone.sign(RP, ORIGIN, forOther, true, true)).status() + ")");
        PasskeyBrowser tamperer = bank.browser();
        SoftwareAuthenticator.Assertion touchOnly = phone.sign(RP, ORIGIN, String.valueOf(tamperer.authenticationOptions().get("challenge")), false, true);
        byte[] edited = B64.decode(touchOnly.authenticatorData());
        edited[32] |= 0x04;
        SoftwareAuthenticator.Assertion flipped = new SoftwareAuthenticator.Assertion(touchOnly.credentialId(), B64.encode(edited), touchOnly.clientDataJson(),
                touchOnly.signature(), touchOnly.userHandle());
        line(out, String.format("    %-34srefused (%d)", "flags " + flags(touchOnly) + " -> " + HEX.formatHex(edited, 32, 33) + " after signing:",
                tamperer.postAssertion(flipped).status()));
    }

    private static void act3Lookalike(StringBuilder out, Running bank, SoftwareAuthenticator phone) throws IOException, InterruptedException {
        line(out, "THREE. A look-alike site gets nothing it can use.");
        PasskeyBrowser victim = bank.browser();
        line(out, "  THE PAGE (a copy of the bank's page):");
        line(out, "    " + LOOKALIKE);
        line(out, "  REQUEST 1. A PASSKEY FOR ITS OWN NAME " + LOOKALIKE_HOST + ":");
        line(out, "    " + victim.signInWithPasskey(phone, true, LOOKALIKE, LOOKALIKE_HOST).text(""));
        line(out, "  REQUEST 2. THE BANK'S REAL CHALLENGE, RP ID " + RP + ":");
        line(out, "    is " + LOOKALIKE_HOST + " equal to " + RP + "?  " + (LOOKALIKE_HOST.equals(RP) ? "yes" : "no"));
        line(out, "    does it end with ." + RP + "?  " + (LOOKALIKE_HOST.endsWith("." + RP) ? "yes" : "no"));
        line(out, "    " + victim.signInWithPasskey(phone, true, LOOKALIKE, RP).text(""));
        PasskeyBrowser.Reply relayed = victim.signInWithPasskeyWithoutBrowserRules(phone, true, LOOKALIKE, RP);
        String signedOrigin = new String(B64.decode(phone.lastAssertion().clientDataJson()), StandardCharsets.UTF_8);
        line(out, "  REQUEST 3. A BROWSER THAT SKIPPED THAT RULE:");
        line(out, "    signed client data: " + signedOrigin.substring(signedOrigin.indexOf("\"origin\""), signedOrigin.indexOf(",\"crossOrigin\"")));
        line(out, "    allowed origins:    [" + ORIGIN + "]");
        line(out, "    refused (" + relayed.status() + ")");
        PasskeyBrowser phished = bank.browser();
        int status = phished.signInWithPassword("ana", PasskeySecurity.PASSWORD).status();
        line(out, "  THE SAME PAGE ASKS FOR ANA'S PASSWORD, AND SHE TYPES IT:");
        line(out, "    replayed at the bank: " + (status == 200 ? "accepted (" + who(phished) + ")" : "refused"));
        line(out, "  a password can be phished; a passkey cannot");
    }

    private static void act4VerificationAndCounters(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. User verification, and counters against copies.");
        try (Running bank = Running.start()) {
            SoftwareAuthenticator phone = registeredPhone(bank, "ana");
            line(out, "  SPRING SECURITY'S DEFAULTS (user verification preferred):");
            PasskeyBrowser b1 = bank.browser();
            PasskeyBrowser.Attempt touch = b1.signInWithPasskey(phone, false, ORIGIN, RP);
            line(out, "    touch only, flags " + flags(phone.lastAssertion()) + ":");
            line(out, "      " + describe(touch, b1) + "  <- NOT REQUIRED");
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            long before = phone.counter();
            phone.setCounter(0);
            PasskeyBrowser b2 = bank.browser();
            PasskeyBrowser.Attempt restored = b2.signInWithPasskey(phone, true, ORIGIN, RP);
            line(out, "    counter " + before + ", then " + counter(phone.lastAssertion()) + " (restored from a backup):");
            line(out, "      " + describe(restored, b2));
            SoftwareAuthenticator clone = phone.cloneIt();
            PasskeyBrowser b3 = bank.browser();
            PasskeyBrowser.Attempt cloned = b3.signInWithPasskey(clone, true, ORIGIN, RP);
            line(out, "    a copied key, counter " + counter(clone.lastAssertion()) + ", as the real phone would:");
            line(out, "      " + describe(cloned, b3) + "  <- NOT NOTICED");
        }
        try (Running bank = Running.start("bank.mode=hardened")) {
            HardenedRelyingParty rp = (HardenedRelyingParty) bank.context()
                    .getBean(org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations.class);
            SoftwareAuthenticator phone = registeredPhone(bank, "ana");
            line(out, "  HARDENED (HardenedRelyingParty around Spring Security's):");
            line(out, "    user verification  " + bank.browser().authenticationOptions().get("userVerification"));
            PasskeyBrowser.Attempt refused = bank.browser().signInWithPasskey(phone, false, ORIGIN, RP);
            line(out, "    touch only, flags " + flags(phone.lastAssertion()) + ":");
            line(out, "      refused (" + rp.refusals().getLast() + ")");
            PasskeyBrowser finger = bank.browser();
            PasskeyBrowser.Attempt verified = finger.signInWithPasskey(phone, true, ORIGIN, RP);
            line(out, "    touch and fingerprint, flags " + flags(phone.lastAssertion()) + ":");
            line(out, "      " + describe(verified, finger));
            for (int i = 0; i < 4; i++) {
                bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            }
            CredentialRecord record = bank.credentials().findByCredentialId(Bytes.fromBase64(phone.credentialId()));
            long seen = record.getSignatureCount();
            line(out, "  THE COUNTERS (the bank keeps the last one it saw):");
            line(out, "    the bank has seen " + seen);
            SoftwareAuthenticator clone = phone.cloneIt();
            PasskeyBrowser real = bank.browser();
            PasskeyBrowser.Attempt first = real.signInWithPasskey(phone, true, ORIGIN, RP);
            long c1 = counter(phone.lastAssertion());
            line(out, "    real phone:  counter " + c1 + ", " + c1 + " > " + seen + "?  " + (c1 > seen ? "yes" : "no"));
            line(out, "      " + describe(first, real));
            seen = c1;
            PasskeyBrowser.Attempt cloneAttempt = bank.browser().signInWithPasskey(clone, true, ORIGIN, RP);
            long c2 = counter(clone.lastAssertion());
            line(out, "    copied key:  counter " + c2 + ", " + c2 + " > " + seen + "?  " + (c2 > seen ? "yes" : "no"));
            line(out, "      refused (" + rp.refusals().getLast() + ")");
            PasskeyBrowser.Attempt realAgain = bank.browser().signInWithPasskey(phone, true, ORIGIN, RP);
            line(out, "    real phone again, counter " + counter(phone.lastAssertion()) + ":");
            line(out, "      refused (" + rp.refusals().getLast() + ")");
            SoftwareAuthenticator synced = new SoftwareAuthenticator().synced();
            PasskeyBrowser ben = signedInWithPassword(bank, "ben");
            ben.registerPasskey(synced, "ben's synced passkey", true, ORIGIN, RP);
            line(out, "  A SYNCED PASSKEY (flags 1d: + backup eligible + backed up):");
            for (int i = 0; i < 2; i++) {
                PasskeyBrowser s = bank.browser();
                PasskeyBrowser.Attempt attempt = s.signInWithPasskey(synced, true, ORIGIN, RP);
                line(out, "    flags " + flags(synced.lastAssertion()) + ", counter " + counter(synced.lastAssertion()) + ": " + describe(attempt, s));
            }
            line(out, "  a synced passkey always reports 0: a copy cannot be spotted");
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
            line(out, "  BEN'S WAYS IN:");
            line(out, "    " + registered + " passkeys: his phone and a security key, each with a label");
            line(out, "  BEN'S PHONE IS LOST:");
            line(out, "    new laptop:");
            line(out, "      " + bank.browser().signInWithPasskey(SoftwareAuthenticator.empty(), true, ORIGIN, RP).text(""));
            PasskeyBrowser drawer = bank.browser();
            line(out, "    security key:");
            line(out, "      " + describe(drawer.signInWithPasskey(key, true, ORIGIN, RP), drawer));
            PasskeyBrowser.Reply removed = drawer.send("DELETE", "/webauthn/register/" + phone.credentialId(), null, null);
            line(out, "    DELETE /webauthn/register/<the phone's credential id>:");
            line(out, "      " + removed.status() + " (the lost phone's passkey is removed)");
            line(out, "    a thief with the phone and its fingerprint:");
            line(out, "      " + bank.browser().signInWithPasskey(phone, true, ORIGIN, RP).text(""));
            line(out, "  recovery is now the weakest way in: guard it like a passkey");
            PasskeyBrowser ana = signedInWithPassword(bank, "ana");
            line(out, "  ANA'S OLD PASSWORD, STILL SWITCHED ON:");
            line(out, "    " + (ana.me().status() == 200 ? "accepted (" + who(ana) + ")" : "refused") + "  <- THE WEAKEST WAY IN");
        }
        try (Running bank = Running.start()) {
            PasskeyBrowser ana = signedInWithPassword(bank, "ana");
            ana.registerPasskey(new SoftwareAuthenticator(), "ana's phone", true, ORIGIN, RP);
            ana.registerPasskey(new SoftwareAuthenticator(), "ana's security key", true, ORIGIN, RP);
            line(out, "  ANA'S PASSWORD, SWITCHED OFF (she has " + PasswordSwitchOffFilter.PASSKEYS_NEEDED + " passkeys):");
            line(out, "    refused (" + bank.browser().signInWithPassword("ana", PasskeySecurity.PASSWORD).status() + ")");
            PasskeyBrowser ben = bank.browser();
            line(out, "  BEN, WITH NO PASSKEY, KEEPS HIS PASSWORD:");
            line(out, "    " + (ben.signInWithPassword("ben", PasskeySecurity.PASSWORD).status() == 200 ? "accepted (" + who(ben) + ")" : "refused"));
        }
        line(out, "  Spring Security's WebAuthn support, built on WebAuthn4J");
    }
}
