package com.jk.explore.security.certlife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * The life of a certificate in a bank's private PKI: issued by a root and an intermediate (openssl), revoked and checked by CRL, OCSP and stapling,
 * replaced by short-lived certificates that step-ca renews, and the outage when a renewal is missed. Everything runs on this machine.
 */
public class CertificateLifecycleDemo {

    /** Lifetime of the short-lived certificates, in seconds: a renewal is due after two thirds of it. */
    static final int LIFE = 12;

    public static void main(String[] args) throws Exception {
        System.out.print(run(Path.of("build")));
    }

    static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    public static String run(Path buildDir) throws Exception {
        // The JDK waits 15 seconds for an OCSP responder; the demo waits 1 second (these two values are in seconds).
        System.setProperty("com.sun.security.ocsp.timeout", "1");
        System.setProperty("com.sun.security.ocsp.readtimeout", "1");
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory(Files.createDirectories(buildDir), "certlife-");
        try {
            Openssl ssl = Openssl.find();
            int ocspPort = Responder.reservePort();
            Pki pki = new Pki(ssl, work.resolve("pki"), "http://127.0.0.1:" + ocspPort);
            authorityAndCertificate(out, ssl, pki, ocspPort);
            crlSizes(out, ssl, work.resolve("crl-sizes"));
            stepCa(out, work.resolve("step"));
            fleet(out, ssl, work.resolve("fleet"));
            bill(out);
        } finally {
            deleteTree(work);
        }
        return out.toString();
    }

    // ----------------------------------------------------------------------------------------------------------- the authority and one certificate

    private static void authorityAndCertificate(StringBuilder out, Openssl ssl, Pki pki, int ocspPort) throws Exception {
        X509Certificate root = pki.root();
        X509Certificate inter = pki.intermediate();
        out.append("--- the authority: a root and an intermediate, both made with openssl\n");
        out.append("root: self-signed " + yn(selfSigned(root)) + ", CA " + yn(root.getBasicConstraints() >= 0) + ", path length limit "
                + pathLength(root) + ", key usage " + keyUsage(root) + "\n");
        out.append("intermediate: signed by the root " + yn(signedBy(inter, root)) + ", CA " + yn(inter.getBasicConstraints() >= 0) + ", path length limit "
                + pathLength(inter) + ", key usage " + keyUsage(inter) + ", signature " + inter.getSigAlgName() + "\n");
        out.append("the root key signs only the intermediate; the intermediate key signs every certificate and every CRL and OCSP response below\n");

        Instant now = Instant.now();
        Path key = pki.newKeyAndRequest("api");
        Pki.Issued api = pki.issue("api.bank.example", key, List.of("api.bank.example", "payments.bank.example"), now.minusSeconds(60),
                now.plus(30, ChronoUnit.DAYS), true);
        X509Certificate leaf = api.cert();
        List<X509Certificate> intermediates = List.of(inter);
        out.append("--- a server certificate issued by the intermediate, read in Java\n");
        out.append("names (subject alternative names): " + names(leaf) + "\n");
        out.append("key usage: " + keyUsage(leaf) + "; extended key usage: " + extendedKeyUsage(leaf) + "; CA: " + yn(leaf.getBasicConstraints() >= 0) + "\n");
        out.append("public key: " + leaf.getPublicKey().getAlgorithm() + " " + ((ECPublicKey) leaf.getPublicKey()).getParams().getOrder().bitLength()
                + " bits; signature: " + leaf.getSigAlgName() + "\n");
        out.append("lifetime: " + Duration.between(leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant()).plusMinutes(1).toDays() + " days\n");
        out.append("pointers inside the certificate: OCSP responder " + yn(leaf.getExtensionValue("1.3.6.1.5.5.7.1.1") != null)
                + ", CRL distribution point " + yn(leaf.getExtensionValue("2.5.29.31") != null) + "\n");
        Instant t = Instant.now();
        out.append("chain to the root with the intermediate: " + Chains.validate(leaf, intermediates, root, t, Chains.Source.NONE, Chains.Policy.HARD, List.of()).word() + "\n");
        out.append("chain to the root without the intermediate: " + Chains.validate(leaf, List.of(), root, t, Chains.Source.NONE, Chains.Policy.HARD, List.of()).word() + "\n");
        out.append("same chain 31 days later: " + Chains.validate(leaf, intermediates, root, t.plus(31, ChronoUnit.DAYS), Chains.Source.NONE, Chains.Policy.HARD, List.of()).word() + "\n");
        out.append("same chain a day before it was issued: " + Chains.validate(leaf, intermediates, root, t.minus(1, ChronoUnit.DAYS), Chains.Source.NONE, Chains.Policy.HARD, List.of()).word() + "\n");

        ocsp(out, ssl, pki, ocspPort, root, inter);
        crl(out, pki, root, inter, leaf, api);
        stapling(out, ssl, pki, ocspPort, root);
    }

    // ----------------------------------------------------------------------------------------------------------------------------------- OCSP

    private static void ocsp(StringBuilder out, Openssl ssl, Pki pki, int port, X509Certificate root, X509Certificate inter) throws Exception {
        List<X509Certificate> chain = List.of(inter);
        Instant now = Instant.now();
        Pki.Issued fine = issueFor(pki, "ledger.bank.example", now);
        Pki.Issued stolen = issueFor(pki, "cards.bank.example", now);
        out.append("--- revocation by OCSP: an openssl ocsp responder that is up, down or hanging\n");
        out.append("hard fail = the client refuses when it cannot learn the status; soft fail = it accepts\n");
        try (Responder responder = new Responder(pki, port)) {
            out.append(row("good certificate, responder up", pki, fine, chain, root, responder));
            out.append("OCSP requests the responder saw for those 2 checks: " + responder.requests() + "\n");
            pki.revoke(stolen);
            responder.reload();
            out.append(row("revoked certificate, responder up", pki, stolen, chain, root, responder));
            responder.mode(Responder.Mode.DOWN);
            out.append(row("good certificate, responder down", pki, fine, chain, root, responder));
            out.append(row("revoked certificate, responder down", pki, stolen, chain, root, responder));
            responder.mode(Responder.Mode.HANG);
            long start = System.nanoTime();
            out.append(row("revoked certificate, responder hangs", pki, stolen, chain, root, responder));
            long millis = (System.nanoTime() - start) / 1_000_000;
            out.append("each check of the hanging responder waited about 1 second (the demo's timeout; the JDK default is 15): " + yn(millis > 1800 && millis < 6000) + "\n");
            out.append("a revoked certificate gets through a soft-fail client whenever the answer can be blocked: yes\n");
        }
    }

    private static String row(String label, Pki pki, Pki.Issued cert, List<X509Certificate> chain, X509Certificate root, Responder responder) throws Exception {
        Verdict hard = Chains.validate(cert.cert(), chain, root, Instant.now(), Chains.Source.OCSP, Chains.Policy.HARD, List.of());
        Verdict soft = Chains.validate(cert.cert(), chain, root, Instant.now(), Chains.Source.OCSP, Chains.Policy.SOFT, List.of());
        return label + ": hard fail " + hard.word() + ", soft fail " + soft.word() + "\n";
    }

    private static Pki.Issued issueFor(Pki pki, String name, Instant now) throws Exception {
        Path key = pki.newKeyAndRequest(name.substring(0, name.indexOf('.')));
        return pki.issue(name, key, List.of(name), now.minusSeconds(60), now.plus(30, ChronoUnit.DAYS), true);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------ CRL

    private static void crl(StringBuilder out, Pki pki, X509Certificate root, X509Certificate inter, X509Certificate leaf, Pki.Issued api) throws Exception {
        List<X509Certificate> chain = List.of(inter);
        out.append("--- revocation by CRL: a signed list that the client downloads\n");
        X509CRL before = pki.generateCrl(3600);
        before.verify(inter.getPublicKey());
        int beforeEntries = before.getRevokedCertificates() == null ? 0 : before.getRevokedCertificates().size();
        out.append("CRL signed by the intermediate and verified in Java: yes; entries: " + beforeEntries + "; good for 3600 seconds in this lab (the entry is the card service certificate revoked above)\n");
        pki.revoke(api);
        out.append("the certificate is revoked after that CRL was published; a client holding the old CRL: "
                + Chains.validate(leaf, chain, root, Instant.now(), Chains.Source.CRL, Chains.Policy.HARD, List.of(before)).word() + "\n");
        X509CRL after = pki.generateCrl(3600);
        after.verify(inter.getPublicKey());
        out.append("new CRL: entries " + after.getRevokedCertificates().size() + "; a client holding it: "
                + Chains.validate(leaf, chain, root, Instant.now(), Chains.Source.CRL, Chains.Policy.HARD, List.of(after)).word() + "\n");
        Instant later = Instant.now().plus(2, ChronoUnit.HOURS);
        out.append("client clock 2 hours later, the CRL's next update has passed: hard fail "
                + Chains.validate(leaf, chain, root, later, Chains.Source.CRL, Chains.Policy.HARD, List.of(after)).word() + ", soft fail "
                + Chains.validate(leaf, chain, root, later, Chains.Source.CRL, Chains.Policy.SOFT, List.of(after)).word() + "\n");
        out.append("a revoked certificate stays usable for clients that hold the old list until their next download: up to 3600 seconds in this lab\n");
    }

    // --------------------------------------------------------------------------------------------------------------------------------- CRL size

    private static void crlSizes(StringBuilder out, Openssl ssl, Path dir) throws Exception {
        Pki pki = new Pki(ssl, dir, "http://127.0.0.1:1");
        Instant now = Instant.now();
        out.append("--- how big a CRL gets (the CA database is written with this many revoked entries, then openssl ca writes the CRL)\n");
        int empty = crlSize(pki, 0, 0, now, false);
        for (int n : new int[] {1_000, 10_000, 100_000}) {
            int size = crlSize(pki, n, 0, now, false);
            out.append("revoked entries " + String.format("%,d", n) + ": CRL " + size / 1024 + " KB, " + Math.round((size - empty) / (double) n) + " bytes per entry"
                    + ", a phone at 1 Mbit/s needs " + String.format("%.1f", size * 8 / 1_000_000.0) + " seconds for it\n");
        }
        int marked = crlSize(pki, 100_000, 90_000, now, true);
        out.append("100,000 revoked entries, 90,000 of them for certificates that have expired, after openssl ca -updatedb: CRL " + marked / 1024
                + " KB (openssl keeps revoked entries after expiry; -updatedb only marks valid ones)\n");
        int dropped = crlSize(pki, 10_000, 0, now, false);
        out.append("the same CA if it dropped the 90,000 entries that RFC 5280 lets it drop: CRL " + dropped / 1024 + " KB\n");
        out.append("a CA that drops expired entries keeps the list as long as the lifetimes of the certificates on it: short lifetimes, short lists\n");
    }

    private static int crlSize(Pki pki, int revoked, int expired, Instant now, boolean prune) throws Exception {
        Files.writeString(pki.indexFile(), Crls.index(revoked, expired, now));
        if (prune) {
            pki.updateDatabase();
        }
        return pki.generateCrl(3600).getEncoded().length;
    }

    // ------------------------------------------------------------------------------------------------------------------------------- stapling

    private static void stapling(StringBuilder out, Openssl ssl, Pki pki, int port, X509Certificate root) throws Exception {
        Instant now = Instant.now();
        Pki.Issued shop = issueFor(pki, "shop.bank.example", now);
        Path staple = pki.ocspResponseFor(shop, "staple-1");
        out.append("--- OCSP stapling: the server sends a signed, dated OCSP response inside the TLS handshake (openssl s_server -status_file)\n");
        out.append("size of the response: under 512 bytes " + yn(Files.size(staple) < 512) + "\n");
        try (Responder responder = new Responder(pki, port)) {
            try (StaplingServer plain = new StaplingServer(ssl, pki, shop, null);
                    StaplingServer stapled = new StaplingServer(ssl, pki, shop, staple)) {
                responder.resetCount();
                StapleClient viaStaple = new StapleClient(root, Chains.Policy.HARD, true, Instant.now());
                StapleClient.Outcome o = viaStaple.connect(stapled.port());
                out.append("server that staples, responder up: " + o.verdict().word() + ", responses in the handshake " + o.stapledResponses()
                        + ", requests to the responder " + responder.requests() + "\n");
                responder.resetCount();
                o = viaStaple.connect(plain.port());
                out.append("server that does not staple, responder up: " + o.verdict().word() + ", responses in the handshake " + o.stapledResponses()
                        + ", requests to the responder " + responder.requests() + " (the responder learns which site the client visits)\n");
                responder.mode(Responder.Mode.DOWN);
                o = viaStaple.connect(plain.port());
                out.append("server that does not staple, responder down: hard fail " + o.verdict().word() + ", soft fail "
                        + new StapleClient(root, Chains.Policy.SOFT, true, Instant.now()).connect(plain.port()).verdict().word() + "\n");
                o = viaStaple.connect(stapled.port());
                out.append("server that staples, responder down: hard fail " + o.verdict().word() + "\n");
                o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now().plus(2, ChronoUnit.DAYS)).connect(stapled.port());
                out.append("same staple read 2 days later, past its next update, responder down: hard fail " + o.verdict().word() + "\n");
                responder.mode(Responder.Mode.ANSWER);
                pki.revoke(shop);
                responder.reload();
                responder.resetCount();
                o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now()).connect(stapled.port());
                out.append("certificate revoked, the server still sends the staple made before: " + o.verdict().word() + ", requests to the responder "
                        + responder.requests() + "\n");
                o = new StapleClient(root, Chains.Policy.HARD, false, Instant.now()).connect(stapled.port());
                out.append("same server, a client that ignores the staple and asks the responder: " + o.verdict().word() + "\n");
            }
            Path fresh = pki.ocspResponseFor(shop, "staple-2");
            try (StaplingServer refreshed = new StaplingServer(ssl, pki, shop, fresh)) {
                StapleClient.Outcome o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now()).connect(refreshed.port());
                out.append("server with a staple made after the revocation: " + o.verdict().word() + "\n");
            }
        }
        out.append("a staple moves the question to the server and survives a dead responder; it is as old as its last refresh (1 day in this lab)\n");
    }

    // -------------------------------------------------------------------------------------------------------------------------------- step-ca

    private static void stepCa(StringBuilder out, Path dir) throws Exception {
        out.append("--- short-lived certificates from step-ca, renewed after two thirds of their life\n");
        try (StepCa ca = new StepCa(dir)) {
            X509Certificate caRoot = ca.rootCertificate();
            Credential first = ca.issue("svc0", "ledger.bank.example", LIFE);
            long life = Duration.between(first.leaf().getNotBefore().toInstant(), first.leaf().getNotAfter().toInstant()).toSeconds();
            out.append("lifetime of each certificate: " + LIFE + " seconds (as issued: within a second of that " + yn(Math.abs(life - LIFE) <= 1) + "), chain sent: "
                    + first.chain().size() + " certificates\n");
            out.append("a renewal is due " + LIFE * 2 / 3 + " seconds after issue; a stolen certificate is worth at most " + LIFE + " seconds instead of 30 days\n");

            try (Service service = new Service(first); Renewer renewer = new Renewer(ca, service, "svc0"); ExpiryWatch watch = new ExpiryWatch(service.port())) {
                Load load = new Load(caRoot, "ledger.bank.example", service.port());
                Load.Tally tally = load.run(300, 100);
                out.append("window of 30 seconds, one handshake every 100 ms: " + tally.attempts() + " handshakes, " + tally.refused() + " failed\n");
                out.append("renewals in the window: " + renewer.renewed() + ", failed renewals: " + renewer.failed() + "\n");
                out.append("expiry alerts raised while rotation works: " + (watch.firstAlert() == null ? 0 : 1) + "\n");
                out.append("the service now presents a different certificate than at the start: "
                        + yn(!service.credential().leaf().getSerialNumber().equals(first.leaf().getSerialNumber())) + "\n");
            }

            hiccup(out, ca, caRoot);
            missed(out, ca, caRoot);
        }
    }

    /** The CA is unreachable for less than the third of the life that is left when the renewal falls due. */
    private static void hiccup(StringBuilder out, StepCa ca, X509Certificate caRoot) throws Exception {
        Credential cred = ca.issue("hic0", "ledger.bank.example", LIFE);
        try (Service service = new Service(cred); Renewer renewer = new Renewer(ca, service, "hic0")) {
            Load load = new Load(caRoot, "ledger.bank.example", service.port());
            CompletableFuture<Load.Tally> run = CompletableFuture.supplyAsync(() -> {
                try {
                    return load.run(200, 100);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });
            Thread.sleep(7000);
            ca.stop();
            Thread.sleep(1500);
            ca.start();
            Load.Tally tally = run.get();
            out.append("--- the CA is unreachable for about 2 seconds, just before a renewal is due (the headroom is " + LIFE / 3 + " seconds)\n");
            out.append("handshakes in 20 seconds: " + tally.attempts() + ", failed: " + tally.refused() + "\n");
            out.append("renewal attempts that failed while the CA was away: " + yn(renewer.failed() > 0) + ", renewed once it was back: " + yn(renewer.renewed() > 0) + "\n");
        }
    }

    /** The CA stays away past the end of the certificate: nobody renews, and the service stops answering. */
    private static void missed(StringBuilder out, StepCa ca, X509Certificate caRoot) throws Exception {
        Credential cred = ca.issue("miss0", "ledger.bank.example", LIFE);
        out.append("--- a missed renewal: the CA stays down past the end of the certificate\n");
        try (Service service = new Service(cred); Renewer renewer = new Renewer(ca, service, "miss0"); ExpiryWatch watch = new ExpiryWatch(service.port())) {
            ca.stop();
            Load load = new Load(caRoot, "ledger.bank.example", service.port());
            Load.Tally before = load.run(50, 100);
            out.append("before the expiry: " + before.attempts() + " handshakes, " + before.refused() + " refused\n");
            long wait = Duration.between(Instant.now(), cred.leaf().getNotAfter().toInstant().plusSeconds(1)).toMillis();
            Thread.sleep(Math.max(0, wait));
            Load.Tally after = load.run(100, 100);
            out.append("after the expiry: " + after.attempts() + " handshakes, " + after.refused() + " refused, the reason: " + after.firstReason() + "\n");
            out.append("renewal attempts that failed: more than 10 " + yn(renewer.failed() > 10) + ", renewals: " + renewer.renewed() + "\n");
            Instant alert = watch.firstAlert();
            boolean early = alert != null && after.firstRefusal() != null && alert.isBefore(after.firstRefusal());
            out.append("the expiry monitor (an alert at a quarter of the life left) alerted before the first refused handshake: " + yn(early)
                    + ", at least 2 seconds before: " + yn(early && Duration.between(alert, after.firstRefusal()).toMillis() >= 2000) + "\n");
            renewer.close();
            ca.start();
            Path crt = ca.file("miss0.crt");
            out.append("the CA is back; renewing the expired certificate: refused " + yn(!ca.tryRenew("miss0", "miss-again").ok()) + " (it has to be issued anew)\n");
            Credential reissued = ca.issue("miss1", "ledger.bank.example", LIFE);
            service.install(reissued);
            Load.Tally recovered = load.run(20, 100);
            out.append("after a new certificate is installed: " + recovered.attempts() + " handshakes, " + recovered.refused() + " refused\n");
            if (!Files.exists(crt)) {
                throw new IllegalStateException("lost the certificate file");
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------------- fleet

    private static void fleet(StringBuilder out, Openssl ssl, Path dir) throws Exception {
        Pki pki = new Pki(ssl, dir, "http://127.0.0.1:1");
        Path key = pki.newKeyAndRequest("fleet");
        Instant start = Instant.now();
        List<Double> daysLeft = new ArrayList<>();
        for (double d : new double[] {-40.5, -9.5, -3.5, -0.5}) {
            daysLeft.add(d);
        }
        for (int i = 0; i < 6; i++) {
            daysLeft.add(0.5 + i);
        }
        for (int i = 0; i < 14; i++) {
            daysLeft.add(8.5 + i * 1.5);
        }
        for (int i = 0; i < 36; i++) {
            daysLeft.add(31.5 + i * 5);
        }
        Path inventory = Files.createDirectories(dir.resolve("inventory"));
        int n = 0;
        for (double d : daysLeft) {
            Instant notAfter = start.plusSeconds((long) (d * 86400));
            Pki.Issued c = pki.issue("service-%03d.bank.example".formatted(++n), key, List.of("service-%03d.bank.example".formatted(n)), notAfter.minus(90, ChronoUnit.DAYS), notAfter, false);
            Files.copy(c.certFile(), inventory.resolve("service-%03d.crt".formatted(n)));
        }
        List<Fleet.Entry> entries = Fleet.scan(inventory);
        out.append("--- an inventory of " + entries.size() + " certificates, scanned by an expiry monitor\n");
        Map<Fleet.Bucket, Integer> counts = Fleet.count(entries, Instant.now());
        for (Fleet.Bucket b : Fleet.Bucket.values()) {
            out.append("  " + b.label + ": " + counts.get(b) + "\n");
        }
        out.append("the first scan finds " + counts.get(Fleet.Bucket.EXPIRED) + " certificates that are already refused by clients, before anyone reported them\n");
        out.append("model: 60 days, a scan every morning, a certificate expiring in that window is an outage unless its owner renews in time\n");
        Instant now = Instant.now();
        Fleet.Model none = Fleet.model(entries, now, 60, 0, 0, false);
        out.append("expiring inside the 60 days: " + none.expiringInHorizon() + "\n");
        out.append("no monitor: " + none.outages() + " outages\n");
        for (int[] cfg : new int[][] {{7, 3}, {7, 10}, {30, 10}}) {
            Fleet.Model m = Fleet.model(entries, now, 60, cfg[0], cfg[1], true);
            out.append("alert under " + cfg[0] + " days, owner needs " + cfg[1] + " days: " + m.outages() + " outages, " + m.prevented() + " prevented\n");
        }
        out.append("an alert is worth the time the owner needs: a 7 day alert does not help a team with a 10 day change process\n");
    }

    // ------------------------------------------------------------------------------------------------------------------------------------ bill

    private static void bill(StringBuilder out) {
        out.append("--- the bill\n");
        out.append("revocation is a question that needs an answer: with hard fail a dead responder is an outage, with soft fail it is a bypass\n");
        out.append("a CRL is big and old, an OCSP answer is small and live but must be asked for, a staple is small and moves the ask to the server\n");
        out.append("a short life makes revocation almost unnecessary and turns renewal into the thing that must never fail\n");
        out.append("the headroom for a CA outage is a third of the life; a monitor that counts the days left is the last line\n");
    }

    // ----------------------------------------------------------------------------------------------------------------------------------- helpers

    static boolean selfSigned(X509Certificate c) throws GeneralSecurityException {
        return signedBy(c, c) && c.getSubjectX500Principal().equals(c.getIssuerX500Principal());
    }

    static boolean signedBy(X509Certificate c, X509Certificate issuer) {
        try {
            c.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    static String pathLength(X509Certificate ca) {
        int len = ca.getBasicConstraints();
        return len == Integer.MAX_VALUE ? "none" : Integer.toString(len);
    }

    static String keyUsage(X509Certificate c) {
        boolean[] ku = c.getKeyUsage();
        String[] names = {"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign"};
        List<String> on = new ArrayList<>();
        for (int i = 0; ku != null && i < names.length; i++) {
            if (ku[i]) {
                on.add(names[i]);
            }
        }
        return String.join(" and ", on);
    }

    static String extendedKeyUsage(X509Certificate c) throws GeneralSecurityException {
        List<String> oids = c.getExtendedKeyUsage();
        if (oids == null) {
            return "none";
        }
        return String.join(", ", oids.stream().map(o -> switch (o) {
            case "1.3.6.1.5.5.7.3.1" -> "serverAuth";
            case "1.3.6.1.5.5.7.3.2" -> "clientAuth";
            default -> o;
        }).toList());
    }

    static String names(X509Certificate c) throws GeneralSecurityException {
        return String.join(", ", c.getSubjectAlternativeNames().stream().map(e -> String.valueOf(e.get(1))).toList());
    }

    static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
