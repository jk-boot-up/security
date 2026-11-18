package com.jk.explore.security.pinning;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Runs the pinning story in five acts and prints exact, repeatable results: the clock is fixed and the keys come from a seeded generator. */
public class PinningDemo {

    static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    static final String HOST = "api.bank.example";
    static final long MAX_AGE = 31_536_000;

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        Pki.seed(2026);
        StringBuilder out = new StringBuilder();
        Pki.Identity publicRoot = Pki.root("Public Root CA", NOW);
        Pki.Identity intermediate = Pki.intermediate("Public Issuing CA", publicRoot, NOW);
        KeyPair serverKey = Pki.newKey();
        KeyPair backupKey = Pki.newKey(); // generated now, kept offline, not in use yet
        X509Certificate leaf = Pki.leaf(HOST, intermediate, serverKey, NOW, Duration.ofDays(90), List.of(HOST));
        List<X509Certificate> chain = List.of(leaf, intermediate.certificate());

        Pki.Identity rogueRoot = Pki.root("Helpful Proxy Root", NOW); // installed on the device by a company, a tool or malware
        Pki.Identity rogueIssuer = Pki.intermediate("Helpful Proxy CA", rogueRoot, NOW);
        List<X509Certificate> interception = List.of(Pki.leaf(HOST, rogueIssuer, Pki.newKey(), NOW, Duration.ofDays(1), List.of(HOST)), rogueIssuer.certificate());

        Set<X509Certificate> cleanDevice = Set.of(publicRoot.certificate());
        Set<X509Certificate> proxiedDevice = Set.of(publicRoot.certificate(), rogueRoot.certificate());

        String leafPin = Spki.pin(leaf);
        String issuerPin = Spki.pin(intermediate.certificate());
        String backupPin = Spki.pin(backupKey.getPublic());
        KEY_NAMES.clear();
        KEY_NAMES.put(leafPin, "the leaf key");
        KEY_NAMES.put(issuerPin, "the issuer's key");
        KEY_NAMES.put(backupPin, "the backup key");

        head(out, "ONE. An app that trusts the device.");
        head(out, "THE DEVICE'S ROOTS (what the phone trusts):");
        line(out, pad("Public Root CA", 20) + "shipped with the phone");
        line(out, pad("Helpful Proxy Root", 20) + "added by a proxy, a tool or malware");
        head(out, "THE CHAINS (leaf first, as each server sends it):");
        line(out, pad("genuine", 14) + describe(chain));
        line(out, pad("interception", 14) + describe(interception));
        head(out, "THE CHECK, BY HAND (signed by a root on the phone?):");
        line(out, pad("genuine", 14) + anchor(chain, proxiedDevice));
        line(out, pad("interception", 14) + anchor(interception, proxiedDevice));
        head(out, "THE RESULT (the app has no pins):");
        PinnedClient trusting = new PinnedClient(proxiedDevice, null);
        line(out, pad("genuine", 14) + trusting.connect(chain, HOST, NOW));
        line(out, pad("interception", 14) + trusting.connect(interception, HOST, NOW) + "  <- THE PROXY READS IT ALL");

        head(out, "TWO. An app that pins the server's key.");
        PinSteps steps = PinSteps.of(serverKey.getPublic());
        head(out, "THE INPUT (the leaf's public key, EC P-256):");
        line(out, "SubjectPublicKeyInfo  " + steps.spki().length + " bytes = " + steps.headerBytes() + " header + 65 point");
        head(out, "THE PIN, WORKED OUT:");
        line(out, "SHA-256 of the " + steps.spki().length + " bytes  " + steps.hash().length + " bytes  " + steps.hashHex().substring(0, 12) + "...");
        line(out, "Base64 of the hash  " + steps.pin().length() + " chars, the pin:");
        line(out, "  " + steps.pin());
        head(out, "THE BASE64, BY HAND (3 bytes -> 4 letters of 6 bits):");
        int[] six = steps.firstGroup();
        line(out, "bytes    " + steps.hashHex().substring(0, 2) + " " + steps.hashHex().substring(2, 4) + " " + steps.hashHex().substring(4, 6));
        StringBuilder bits = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder letters = new StringBuilder();
        for (int v : six) {
            bits.append(String.format("%6s", Integer.toBinaryString(v)).replace(' ', '0')).append(' ');
            values.append(pad(String.valueOf(v), 7));
            letters.append(pad(String.valueOf(PinSteps.ALPHABET.charAt(v)), 7));
        }
        line(out, "6 bits   " + bits.toString().trim());
        line(out, "values   " + values.toString().trim());
        line(out, "letters  " + letters.toString().trim());
        line(out, "32 bytes = 10 x 3 + 2 left -> 11 groups x 4 = 44 chars");
        line(out, "same as the JDK's Base64: " + yesNo(steps.pin().equals(leafPin)));
        head(out, "THE CHECK, BY HAND (is a key of the chain pinned?):");
        Set<String> leafOnly = Set.of(leafPin);
        line(out, pad("genuine", 14) + pinned(chain, leafOnly));
        line(out, pad("interception", 14) + pinned(interception, leafOnly));
        head(out, "THE RESULT (the app ships the leaf's pin):");
        PinnedClient pinnedClient = new PinnedClient(proxiedDevice, PinPolicy.of(leafPin));
        line(out, pad("genuine", 27) + pinnedClient.connect(chain, HOST, NOW));
        line(out, pad("interception", 27) + pinnedClient.connect(interception, HOST, NOW));
        line(out, pad("interception, clean phone", 27) + new PinnedClient(cleanDevice, PinPolicy.of(leafPin)).connect(interception, HOST, NOW));

        head(out, "THREE. The server renews with a new key.");
        Instant renewal = NOW.plus(Duration.ofDays(80));
        KeyPair newKey = Pki.newKey();
        List<X509Certificate> renewed = List.of(Pki.leaf(HOST, intermediate, newKey, renewal, Duration.ofDays(90), List.of(HOST)), intermediate.certificate());
        head(out, "THE RENEWAL (day 80: a new certificate, a new key):");
        line(out, pad("the app's pin", 19) + short12(leafPin));
        line(out, pad("the new key's pin", 19) + short12(Spki.pin(renewed.get(0))));
        head(out, "THE CHECK, BY HAND (is a key of the chain pinned?):");
        line(out, pad("renewed chain", 18) + pinned(renewed, leafOnly));
        head(out, "THE RESULT (the app pinned to the old leaf key):");
        line(out, pad("renewed chain", 18) + new PinnedClient(proxiedDevice, PinPolicy.of(leafPin)).connect(renewed, HOST, renewal) + "  <- THE BANK'S OWN APP");
        Fleet fleet = new Fleet(10_000, 85);
        head(out, "THE FLEET, WORKED OUT (" + fleet.installs() + " installs, " + fleet.percentUpdatedWithinAWeek() + "% update in a week):");
        line(out, pad("locked out on day 0", 22) + fleet.lockedOutAtDay0());
        line(out, pad("after a week", 22) + fleet.installs() + " - " + fleet.installs() + " x " + fleet.percentUpdatedWithinAWeek() + " / 100 = " + fleet.lockedOutAfterAWeek());

        head(out, "FOUR. Pins chosen so that rotation does not break the app.");
        X509Certificate switched = Pki.leaf(HOST, intermediate, backupKey, renewal, Duration.ofDays(90), List.of(HOST));
        List<X509Certificate> backupChain = List.of(switched, intermediate.certificate());
        head(out, "THE PINS (what each app ships):");
        line(out, pad("issuer pin", 15) + "the Public Issuing CA's key " + short12(issuerPin));
        line(out, pad("leaf + backup", 15) + "the leaf key, and a spare kept offline");
        line(out, pad("report-only", 15) + "the leaf key; a mismatch is only reported");
        head(out, "THE CHECK, BY HAND (which key of the chain is pinned?):");
        Set<String> issuerOnly = Set.of(issuerPin);
        Set<String> leafAndBackup = Set.of(leafPin, backupPin);
        line(out, pad("issuer pin, renewed", 26) + pinned(renewed, issuerOnly));
        line(out, pad("issuer pin, interception", 26) + pinned(interception, issuerOnly));
        line(out, pad("backup pin, key switched", 26) + pinned(backupChain, leafAndBackup));
        line(out, pad("report-only, renewed", 26) + pinned(renewed, leafOnly));
        head(out, "THE RESULT:");
        line(out, pad("issuer pin, renewed", 26) + new PinnedClient(proxiedDevice, PinPolicy.of(issuerPin)).connect(renewed, HOST, renewal));
        line(out, pad("issuer pin, interception", 26) + new PinnedClient(proxiedDevice, PinPolicy.of(issuerPin)).connect(interception, HOST, NOW));
        line(out, pad("backup pin, key switched", 26) + new PinnedClient(proxiedDevice, PinPolicy.of(leafPin, backupPin)).connect(backupChain, HOST, renewal));
        PinnedClient report = new PinnedClient(proxiedDevice, PinPolicy.of(leafPin).inReportOnlyMode());
        line(out, pad("report-only, renewed", 26) + report.connect(renewed, HOST, renewal));
        line(out, pad("reports collected", 26) + report.reports().size());

        head(out, "FIVE. Why browsers stopped letting servers set pins.");
        head(out, "THE HEADER (a meddler answers one first visit, HPKP):");
        line(out, "Public-Key-Pins: pin-sha256=\"" + short12(Spki.pin(rogueIssuer.certificate())) + "\";");
        line(out, "  pin-sha256=\"" + short12(Spki.pin(rogueRoot.certificate())) + "\"; max-age=" + MAX_AGE);
        PinStore store = new PinStore();
        String header = "pin-sha256=\"" + Spki.pin(rogueIssuer.certificate()) + "\"; pin-sha256=\"" + Spki.pin(rogueRoot.certificate()) + "\"; max-age=" + MAX_AGE;
        store.onResponse(HOST, header, NOW);
        long days = MAX_AGE / 86_400;
        head(out, "THE max-age, WORKED OUT:");
        line(out, MAX_AGE + " s / 86400 s a day = " + days + " days");
        head(out, "THE CHECK, BY HAND (is the visit within " + days + " days?):");
        line(out, pad("day 30", 9) + "30 < " + days + ": the meddler's pins still apply");
        line(out, pad("day 400", 9) + "400 > " + days + ": the pins have expired");
        head(out, "THE RESULT (the genuine bank server):");
        line(out, pad("day 30", 9) + (store.allows(HOST, chain, NOW.plus(Duration.ofDays(30))) ? "accepted" : "blocked for every returning visitor"));
        line(out, pad("day 400", 9) + (store.allows(HOST, chain, NOW.plus(Duration.ofDays(400))) ? "accepted again" : "blocked"));
        PinStore own = new PinStore();
        own.onResponse(HOST, "pin-sha256=\"" + leafPin + "\"; pin-sha256=\"" + backupPin + "\"; max-age=" + MAX_AGE, NOW);
        head(out, "THE SITE'S OWN PINS (leaf + backup), THEN A NEW KEY:");
        line(out, pad("day 80", 9) + (own.allows(HOST, List.of(renewed.get(0), intermediate.certificate()), NOW.plus(Duration.ofDays(80)))
                ? "accepted" : "blocked until day " + days + "  <- NO WAY TO UNDO IT"));
        return out.toString();
    }

    /** What the demo calls each pinned key, by its pin. */
    private static final java.util.Map<String, String> KEY_NAMES = new java.util.HashMap<>();

    private static void head(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void line(StringBuilder out, String text) {
        out.append("  ").append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private static String short12(String pin) {
        return pin.substring(0, 12) + "...";
    }

    /** The chain as subject names, leaf first, each signed by the next. */
    private static String describe(List<X509Certificate> chain) {
        return chain.get(0).getSubjectX500Principal().getName().replaceFirst("^CN=", "") + " <- " + ChainWalk.name(chain.get(1).getSubjectX500Principal());
    }

    private static String anchor(List<X509Certificate> chain, Set<X509Certificate> roots) {
        return ChainWalk.anchor(chain, roots).map(r -> "yes, by " + ChainWalk.name(r.getSubjectX500Principal())).orElse("no");
    }

    private static String pinned(List<X509Certificate> chain, Set<String> pins) {
        return ChainWalk.pinned(chain, pins).map(c -> "yes, " + KEY_NAMES.get(Spki.pin(c)) + " " + short12(Spki.pin(c))).orElse("none of " + chain.size() + " keys");
    }
}
