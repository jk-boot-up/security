package com.jk.explore.security.pinning;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Runs the pinning story in five numbered acts and prints exact, repeatable results. Keys are random and never printed. */
public class PinningDemo {

    static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    static final String HOST = "api.bank.example";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
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

        out.append("== 1. An app that trusts the device ==\n");
        PinnedClient trusting = new PinnedClient(proxiedDevice, null);
        out.append(line("genuine server", trusting, chain));
        out.append(line("interception by the device's extra root", trusting, interception));

        out.append("\n== 2. An app that pins the server's key ==\n");
        PinnedClient pinned = new PinnedClient(proxiedDevice, PinPolicy.of(leafPin));
        out.append(line("genuine server", pinned, chain));
        out.append(line("interception by the device's extra root", pinned, interception));
        out.append(line("interception on a clean device", new PinnedClient(cleanDevice, PinPolicy.of(leafPin)), interception));

        out.append("\n== 3. The server renews with a new key ==\n");
        Instant renewal = NOW.plus(Duration.ofDays(80));
        KeyPair newKey = Pki.newKey();
        List<X509Certificate> renewed = List.of(Pki.leaf(HOST, intermediate, newKey, renewal, Duration.ofDays(90), List.of(HOST)), intermediate.certificate());
        out.append(line("app pinned to the old leaf key", new PinnedClient(proxiedDevice, PinPolicy.of(leafPin)), renewed, renewal));
        Fleet fleet = new Fleet(10_000, 85);
        out.append("fleet of ").append(fleet.installs()).append(" installs, 85% update within a week -> locked out on day 0: ").append(fleet.lockedOutAtDay0())
                .append(", after a week: ").append(fleet.lockedOutAfterAWeek()).append('\n');

        out.append("\n== 4. Pins chosen so that rotation does not break the app ==\n");
        out.append(line("pin the issuing CA's key", new PinnedClient(proxiedDevice, PinPolicy.of(issuerPin)), renewed, renewal));
        out.append(line("interception, same app", new PinnedClient(proxiedDevice, PinPolicy.of(issuerPin)), interception, NOW));
        X509Certificate switched = Pki.leaf(HOST, intermediate, backupKey, renewal, Duration.ofDays(90), List.of(HOST));
        out.append(line("pin leaf + backup, server switches to the backup key", new PinnedClient(proxiedDevice, PinPolicy.of(leafPin, backupPin)),
                List.of(switched, intermediate.certificate()), renewal));
        PinnedClient report = new PinnedClient(proxiedDevice, PinPolicy.of(leafPin).inReportOnlyMode());
        out.append(line("report-only mode, after a key change", report, renewed, renewal));
        out.append("reports collected -> ").append(report.reports().size()).append('\n');

        out.append("\n== 5. Why browsers stopped letting servers set pins ==\n");
        PinStore store = new PinStore();
        String header = "pin-sha256=\"" + Spki.pin(rogueIssuer.certificate()) + "\"; pin-sha256=\"" + Spki.pin(rogueRoot.certificate()) + "\"; max-age=31536000";
        store.onResponse(HOST, header, NOW);
        out.append("a meddler answers one request with a pin header for the meddler's keys, max-age one year\n");
        out.append("the genuine server, 30 days later -> ").append(store.allows(HOST, chain, NOW.plus(Duration.ofDays(30))) ? "accepted" : "blocked for every returning visitor").append('\n');
        out.append("the genuine server, 400 days later -> ").append(store.allows(HOST, chain, NOW.plus(Duration.ofDays(400))) ? "accepted again" : "blocked").append('\n');
        PinStore own = new PinStore();
        own.onResponse(HOST, "pin-sha256=\"" + leafPin + "\"; pin-sha256=\"" + backupPin + "\"; max-age=31536000", NOW);
        out.append("a site pins its own key and then loses it -> ").append(own.allows(HOST, List.of(renewed.get(0), intermediate.certificate()), NOW.plus(Duration.ofDays(80)))
                ? "accepted" : "blocked for up to a year").append('\n');
        return out.toString();
    }

    private static String line(String label, PinnedClient c, List<X509Certificate> chain) {
        return line(label, c, chain, NOW);
    }

    private static String line(String label, PinnedClient c, List<X509Certificate> chain, Instant at) {
        return String.format("%-54s -> %s%n", label, c.connect(chain, HOST, at));
    }
}
