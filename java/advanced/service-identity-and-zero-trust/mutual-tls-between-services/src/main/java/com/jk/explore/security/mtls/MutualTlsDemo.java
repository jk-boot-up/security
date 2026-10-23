package com.jk.explore.security.mtls;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MutualTlsDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
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
            return demo(work);
        } finally {
            try (Stream<Path> paths = Files.walk(work)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static String demo(Path work) throws Exception {
        StringBuilder out = new StringBuilder();
        try (Ca ca = new Ca(work)) {
            Path bundle = ca.file("trust.pem");
            Files.copy(ca.root(), bundle);
            List<X509Certificate> clientTrust = Credential.readCertificates(ca.root());

            Credential serverCert = ca.issue("ledger", "ledger", List.of("localhost"), 300);
            Credential payments = ca.issue("payments", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 600);
            Credential reporting = ca.issue("reporting", "svc-reporting-2", List.of(Identities.PREFIX + "reporting"), 600);
            Credential fraud = ca.issue("fraud", "svc-fraud-engine-1", List.of(Identities.PREFIX + "fraud-engine"), 600);

            out.append("--- the private CA and the identity in a certificate\n");
            X509Certificate leaf = payments.leaf();
            out.append("certificates issued by step-ca for the ledger and 3 callers; chain sent by a caller: " + payments.chain().size() + " certificates (leaf and intermediate)\n");
            out.append("subject name: " + leaf.getSubjectX500Principal().getName() + ", identity the ledger uses: " + Identities.of(leaf).orElseThrow() + " (the URI in the SAN, not the name)\n");

            try (Ledger ledger = new Ledger(ca.file("ledger.crt"), ca.file("ledger.p8"), bundle);
                    NetworkGate gate = new NetworkGate()) {
                out.append("--- who gets in\n");
                int served = ledger.served();
                out.append("no client certificate: " + call(null, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                Credential rogue = ca.issueFromOtherCa("rogue", "partner-gateway", Identities.PREFIX + "payments");
                out.append("a certificate from another CA that names itself payments: " + call(rogue, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                out.append("requests that reached the ledger's own code in those two calls: " + (ledger.served() - served) + "\n");
                ca.issue("wrongname", "old-ledger", List.of("ledger-old.bank.internal"), 300);
                try (Ledger wrongName = new Ledger(ca.file("wrongname.crt"), ca.file("wrongname.p8"), bundle)) {
                    out.append("a ledger whose certificate names another host, called by payments (the client checks the server too): "
                            + call(payments, clientTrust, wrongName, "POST", "/transfers").line() + "\n");
                }
                out.append("payments, POST /transfers: " + call(payments, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                out.append("payments, GET /statements: " + call(payments, clientTrust, ledger, "GET", "/statements").line() + "\n");
                out.append("reporting, GET /statements: " + call(reporting, clientTrust, ledger, "GET", "/statements").line() + "\n");
                out.append("reporting, POST /transfers: " + call(reporting, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                out.append("fraud-engine (valid certificate, no rule), POST /transfers: " + call(fraud, clientTrust, ledger, "POST", "/transfers").line() + "\n");

                out.append("--- identity, not the network\n");
                int byNetwork = 0;
                for (int i = 0; i < 2; i++) {
                    byNetwork += Caller.callPlain(gate.url("/transfers"), "POST").status() == 200 ? 1 : 0;
                }
                int byIdentity = 0;
                for (Credential c : List.of(payments, reporting)) {
                    byIdentity += call(c, clientTrust, ledger, "POST", "/transfers").status() == 200 ? 1 : 0;
                }
                out.append("allow the trusted network: payments and reporting both on the same host, POST /transfers allowed for " + byNetwork + " of 2\n");
                out.append("allow by certificate identity: the same two callers, allowed for " + byIdentity + " of 2\n");

                out.append("--- rotation: a certificate that lives 8 seconds\n");
                Credential first = ca.issue("short-a", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 8);
                long life = (first.leaf().getNotAfter().getTime() - first.leaf().getNotBefore().getTime()) / 1000;
                out.append("lifetime of the certificate: " + life + " seconds, accepted now: " + yes(call(first, clientTrust, ledger, "POST", "/transfers").status() == 200) + "\n");
                String before = call(payments, clientTrust, ledger, "POST", "/transfers").serverFingerprint();
                Credential partner = ca.issueFromOtherCa("partner", "partner-gateway", Identities.PREFIX + "payments");
                out.append("a second CA is not in the server's trust file: " + call(partner, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                Thread.sleep(3500);
                try (Traffic traffic = new Traffic(first, clientTrust, url(ledger, "/transfers"))) {
                    Thread.sleep(300);
                    Credential second = ca.renew("short-a", "short-b");
                    traffic.use(second);
                    out.append("renewed halfway through its life: new serial number " + yes(!second.leaf().getSerialNumber().equals(first.leaf().getSerialNumber()))
                            + ", later expiry " + yes(second.leaf().getNotAfter().after(first.leaf().getNotAfter())) + "\n");
                    ca.renew("ledger", "ledger-next");
                    Files.move(ca.file("ledger-next.crt"), ca.file("ledger.crt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    Files.writeString(bundle, Files.readString(bundle) + Files.readString(ca.otherRoot()));
                    ledger.reload();
                    String after = call(payments, clientTrust, ledger, "POST", "/transfers").serverFingerprint();
                    out.append("the server reloaded its certificate and trust file without a restart: clients now see a new server certificate " + yes(!before.equals(after)) + "\n");
                    out.append("the second CA after adding it to the trust file: " + call(partner, clientTrust, ledger, "POST", "/transfers").line() + " (it can now mint any identity, payments included)\n");
                    while (System.currentTimeMillis() < first.leaf().getNotAfter().getTime() + 1500) {
                        Thread.sleep(100);
                    }
                    int beforeExpired = ledger.served();
                    out.append("the first certificate after its expiry: " + call(first, clientTrust, ledger, "POST", "/transfers").line()
                            + ", requests that reached the ledger's own code: " + (ledger.served() - beforeExpired) + "\n");
                    out.append("the renewed certificate: " + call(second, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                    traffic.close();
                    out.append("calls made on new connections during the renewal and the reload: at least 20 " + yes(traffic.succeeded.get() >= 20) + ", failed " + traffic.failed.get() + "\n");
                }

                out.append("--- a compromised certificate\n");
                Credential stolen = ca.issue("stolen", "svc-payments-7", List.of(Identities.PREFIX + "payments"), 600);
                out.append("a copy of the certificate and key, used from another process: " + call(stolen, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                ca.revoke("stolen");
                Set<BigInteger> listed = ca.revokedSerials();
                out.append("revoked at the CA, on its revocation list: " + yes(listed.contains(stolen.leaf().getSerialNumber())) + "\n");
                out.append("the same certificate at the ledger afterwards: " + call(stolen, clientTrust, ledger, "POST", "/transfers").line() + "\n");
                ledger.revoke(listed);
                Caller.Reply refused = call(stolen, clientTrust, ledger, "POST", "/transfers");
                out.append("after the ledger loads the revocation list: " + refused.line() + ", " + refused.body() + "\n");
                out.append("the payments certificate that was not revoked: " + call(payments, clientTrust, ledger, "POST", "/transfers").line() + "\n");

                out.append("--- the bill\n");
                out.append("the CA records a revocation but a server that does not read the list never learns of it: the certificate works until it expires\n");
                out.append("a short lifetime is the revocation that always works, and it costs a renewal job that must not fail\n");
                out.append("a CA in the trust file can mint any identity: the trust file is part of the policy\n");
            }
        }
        return out.toString();
    }
}
