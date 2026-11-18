package com.jk.explore.security.mtls;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Mutual TLS between a bank's services: a real private CA (step-ca), a ledger service on the JDK's HTTPS server that demands a client certificate and
 * decides by the identity in it, and callers using the JDK's HttpClient. Keys, serial numbers, ports and times change on every run, so the demo prints
 * names, lifetimes, answers and comparisons only.
 */
public class MutualTlsDemo {

    private final StringBuilder out = new StringBuilder();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private void line(String format, Object... args) {
        out.append(String.format(format, args)).append('\n');
    }

    private static String url(Ledger ledger, String path) {
        return "https://localhost:" + ledger.port() + path;
    }

    private static Caller.Reply call(Credential who, List<X509Certificate> trusted, Ledger ledger, String method, String path) throws Exception {
        return Caller.call(who, trusted, url(ledger, path), method);
    }

    public static String run() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("build")), "mtls-");
        try {
            MutualTlsDemo d = new MutualTlsDemo();
            d.demo(work);
            return d.out.toString();
        } finally {
            try (Stream<Path> paths = Files.walk(work)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private void demo(Path work) throws Exception {
        try (Ca ca = new Ca(work)) {
            Path bundle = ca.file("trust.pem");
            Files.copy(ca.root(), bundle);
            List<X509Certificate> clientTrust = Credential.readCertificates(ca.root());

            Credential serverCert = ca.issue("ledger", "ledger", List.of("localhost"), 300);
            Credential payments = ca.issue("payments", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 600);
            Credential reporting = ca.issue("reporting", "svc-reporting-2", List.of(Identities.PREFIX + "reporting"), 600);
            Credential fraud = ca.issue("fraud", "svc-fraud-engine-1", List.of(Identities.PREFIX + "fraud-engine"), 600);
            identity(bundle, serverCert, payments, reporting, fraud);

            try (Ledger ledger = new Ledger(ca.file("ledger.crt"), ca.file("ledger.p8"), bundle);
                    NetworkGate gate = new NetworkGate()) {
                handshake(ca, bundle, clientTrust, ledger, payments);
                authorize(clientTrust, ledger, gate, payments, reporting, fraud);
                rotation(ca, bundle, clientTrust, ledger, payments);
                compromise(ca, clientTrust, ledger, payments);
            }
        }
    }

    // ------------------------------------------------------------------ ONE

    private void identity(Path bundle, Credential server, Credential... callers) throws Exception {
        line("ONE. The identity in a certificate.");
        line("  THE INPUT: WHAT step-ca ISSUED (subject, then the URI SAN):");
        line("    %-13s CN=%-19s %s", "ledger", ChainSteps.cn(server.leaf().getSubjectX500Principal().getName()), String.join(", ", Identities.names(server.leaf())));
        for (Credential c : callers) {
            X509Certificate leaf = c.leaf();
            line("    %-13s CN=%-19s %s", Identities.of(leaf).orElse("?"), ChainSteps.cn(leaf.getSubjectX500Principal().getName()),
                    String.join(", ", Identities.names(leaf)).replace("URI ", ""));
        }
        Credential payments = callers[0];
        List<X509Certificate> roots = Credential.readCertificates(bundle);
        line("  THE CHAIN payments SENDS (%d certificates), CHECKED BY HAND:", payments.chain().size());
        int n = 1;
        for (ChainSteps.Link link : ChainSteps.walk(payments.chain(), roots)) {
            line("    %d %s", n++, link.subject());
            line("      issuer %s, verified: %s", link.issuer(), yes(link.verifies()));
        }
        line("    the root %s: in trust.pem (%d root)", ChainSteps.cn(roots.getFirst().getSubjectX500Principal().getName()), roots.size());
        line("    trusted by hand: %s", yes(ChainSteps.trusted(payments.chain(), roots)));
        line("  HOW THE LEDGER READS THE IDENTITY:");
        X509Certificate leaf = payments.leaf();
        line("    the subject CN=%s: not used", ChainSteps.cn(leaf.getSubjectX500Principal().getName()));
        line("    the SAN %s", String.join(", ", Identities.names(leaf)));
        line("    remove the prefix %s: %s", Identities.PREFIX, Identities.of(leaf).orElseThrow());
        line("  THE KEY AND THE LIFETIME OF payments:");
        long life = (leaf.getNotAfter().getTime() - leaf.getNotBefore().getTime()) / 1000;
        line("    key %s, signed with %s", leaf.getPublicKey().getAlgorithm(), leaf.getSigAlgName());
        line("    notAfter - notBefore = %d seconds = %d minutes", life, life / 60);
    }

    // ------------------------------------------------------------------ TWO

    private void handshake(Ca ca, Path bundle, List<X509Certificate> clientTrust, Ledger ledger, Credential payments) throws Exception {
        line("TWO. Who gets through the handshake.");
        Credential rogue = ca.issueFromOtherCa("rogue", "partner-gateway", Identities.PREFIX + "payments");
        ca.issue("wrongname", "old-ledger", List.of("ledger-old.bank.internal"), 300);
        line("  THE CALLS (POST /transfers):");
        line("    1 no client certificate");
        line("    2 a certificate that names %s", Identities.names(rogue.leaf()).getFirst());
        line("      issued by %s", ChainSteps.cn(rogue.leaf().getIssuerX500Principal().getName()));
        line("    3 payments, to a ledger whose certificate names");
        line("      %s (asked for: localhost)", String.join(", ", Identities.names(Credential.readCertificates(ca.file("wrongname.crt")).getFirst())));
        int served = ledger.served();
        String none = call(null, clientTrust, ledger, "POST", "/transfers").line();
        String other = call(rogue, clientTrust, ledger, "POST", "/transfers").line();
        int reached = ledger.served() - served;
        String wrong;
        try (Ledger wrongName = new Ledger(ca.file("wrongname.crt"), ca.file("wrongname.p8"), bundle)) {
            wrong = call(payments, clientTrust, wrongName, "POST", "/transfers").line();
        }
        line("  THE RESULT:");
        line("    1 %s (the ledger wants a certificate)", none);
        line("    2 %s (its issuer is not in trust.pem)", other);
        line("    requests that reached the ledger's own code: %d", reached);
        line("    3 %s by the client (the name does not fit)", wrong);
    }

    // ------------------------------------------------------------------ THREE

    private void authorize(List<X509Certificate> clientTrust, Ledger ledger, NetworkGate gate, Credential payments, Credential reporting, Credential fraud)
            throws Exception {
        line("THREE. Allowed by identity, not by the network.");
        line("  THE LEDGER'S RULES:");
        line("    POST /transfers  payments");
        line("    GET /statements  reporting");
        line("  THE REQUESTS AND THE ANSWERS:");
        Object[][] calls = {{payments, "POST", "/transfers"}, {payments, "GET", "/statements"}, {reporting, "GET", "/statements"},
            {reporting, "POST", "/transfers"}, {fraud, "POST", "/transfers"}};
        for (Object[] c : calls) {
            Credential who = (Credential) c[0];
            Caller.Reply r = call(who, clientTrust, ledger, (String) c[1], (String) c[2]);
            String id = Identities.of(who.leaf()).orElseThrow();
            line("    %-13s %-16s %s  %s", id, c[1] + " " + c[2], r.line(), r.body().replace(id + " may not", "may not"));
        }
        int byNetwork = 0;
        for (int i = 0; i < 2; i++) {
            byNetwork += Caller.callPlain(gate.url("/transfers"), "POST").status() == 200 ? 1 : 0;
        }
        int byIdentity = 0;
        for (Credential c : List.of(payments, reporting)) {
            byIdentity += call(c, clientTrust, ledger, "POST", "/transfers").status() == 200 ? 1 : 0;
        }
        line("  THE SAME TWO CALLERS ON ONE HOST, POST /transfers:");
        line("    allowed because they are on the network:  %d of 2", byNetwork);
        line("    allowed by the identity in the certificate: %d of 2", byIdentity);
    }

    // ------------------------------------------------------------------ FOUR

    private void rotation(Ca ca, Path bundle, List<X509Certificate> clientTrust, Ledger ledger, Credential payments) throws Exception {
        line("FOUR. Rotation: a certificate that lives 8 seconds.");
        Credential first = ca.issue("short-a", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 8);
        long life = (first.leaf().getNotAfter().getTime() - first.leaf().getNotBefore().getTime()) / 1000;
        line("  THE INPUT:");
        line("    payments' certificate, notAfter - notBefore = %d seconds", life);
        line("    accepted now: %s", yes(call(first, clientTrust, ledger, "POST", "/transfers").status() == 200));
        line("    a caller keeps calling on new connections meanwhile");
        String before = call(payments, clientTrust, ledger, "POST", "/transfers").serverFingerprint();
        Credential partner = ca.issueFromOtherCa("partner", "partner-gateway", Identities.PREFIX + "payments");
        String partnerBefore = call(partner, clientTrust, ledger, "POST", "/transfers").line();
        Thread.sleep(3500);
        try (Traffic traffic = new Traffic(first, clientTrust, url(ledger, "/transfers"))) {
            Thread.sleep(300);
            Credential second = ca.renew("short-a", "short-b");
            traffic.use(second);
            line("  THE RENEWAL, HALFWAY THROUGH ITS LIFE:");
            line("    new serial number: %s, later expiry: %s", yes(!second.leaf().getSerialNumber().equals(first.leaf().getSerialNumber())),
                    yes(second.leaf().getNotAfter().after(first.leaf().getNotAfter())));
            int rootsBefore = Credential.readCertificates(bundle).size();
            ca.renew("ledger", "ledger-next");
            Files.move(ca.file("ledger-next.crt"), ca.file("ledger.crt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.writeString(bundle, Files.readString(bundle) + Files.readString(ca.otherRoot()));
            ledger.reload();
            String after = call(payments, clientTrust, ledger, "POST", "/transfers").serverFingerprint();
            line("  THE SERVER RELOADS, WITHOUT A RESTART:");
            line("    a renewed server certificate: clients see it: %s", yes(!before.equals(after)));
            line("    roots in trust.pem: %d, then %d (Partner Bank CA added)", rootsBefore, Credential.readCertificates(bundle).size());
            line("    Partner Bank CA's payments: %s before, %s after", partnerBefore, call(partner, clientTrust, ledger, "POST", "/transfers").line());
            while (System.currentTimeMillis() < first.leaf().getNotAfter().getTime() + 1500) {
                Thread.sleep(100);
            }
            traffic.close();
            int beforeExpired = ledger.served();
            String expired = call(first, clientTrust, ledger, "POST", "/transfers").line();
            line("  THE RESULT:");
            line("    the first certificate after its expiry: %s", expired);
            line("      requests that reached the ledger's own code: %d", ledger.served() - beforeExpired);
            line("    the renewed certificate: %s", call(second, clientTrust, ledger, "POST", "/transfers").line());
            line("    calls meanwhile: at least 20: %s, failed: %d", yes(traffic.succeeded.get() >= 20), traffic.failed.get());
        }
    }

    // ------------------------------------------------------------------ FIVE

    private void compromise(Ca ca, List<X509Certificate> clientTrust, Ledger ledger, Credential payments) throws Exception {
        line("FIVE. A stolen certificate, and revocation.");
        Credential stolen = ca.issue("stolen", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 600);
        line("  THE INPUT:");
        line("    a copy of payments' certificate and key, from another process");
        line("    it calls POST /transfers: %s", call(stolen, clientTrust, ledger, "POST", "/transfers").line());
        ca.revoke("stolen");
        Set<BigInteger> listed = ca.revokedSerials();
        line("  REVOKED AT THE CA:");
        line("    on the CA's revocation list (CRL): %s, entries %d", yes(listed.contains(stolen.leaf().getSerialNumber())), listed.size());
        line("  THE RESULT AT THE LEDGER:");
        line("    before the ledger reads the list: %s", call(stolen, clientTrust, ledger, "POST", "/transfers").line());
        ledger.revoke(listed);
        Caller.Reply refused = call(stolen, clientTrust, ledger, "POST", "/transfers");
        line("    after it reads the list: %s, %s", refused.line(), refused.body());
        line("    payments' own certificate, not revoked: %s", call(payments, clientTrust, ledger, "POST", "/transfers").line());
        line("  THE BILL:");
        line("    a revocation works only where the list is read");
        line("    a short lifetime is the revocation that always works");
        line("    every CA in trust.pem can mint any identity");
    }
}
