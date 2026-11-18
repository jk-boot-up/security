package com.jk.explore.security.certlife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
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
        StepCa.requirePinnedVersions();
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory(Files.createDirectories(buildDir), "certlife-");
        try {
            Openssl ssl = Openssl.find();
            int ocspPort = Responder.reservePort();
            Pki pki = new Pki(ssl, work.resolve("pki"), "http://127.0.0.1:" + ocspPort);
            Pki.Issued api = issueAndRead(out, pki);
            act(out, "TWO", "Revocation: OCSP, a CRL and a dead responder.");
            ocsp(out, pki, ocspPort);
            crl(out, pki, api);
            crlSizes(out, ssl, work.resolve("crl-sizes"));
            stapling(out, ssl, pki, ocspPort);
            stepCa(out, work.resolve("step"));
            fleet(out, ssl, work.resolve("fleet"));
        } finally {
            deleteTree(work);
        }
        return out.toString();
    }

    // ----------------------------------------------------------------------------------------------------------- ONE. the authority and one certificate

    private static Pki.Issued issueAndRead(StringBuilder out, Pki pki) throws Exception {
        X509Certificate root = pki.root();
        X509Certificate inter = pki.intermediate();
        act(out, "ONE", "Issue and read: a root, an intermediate, a certificate.");
        head(out, "THE AUTHORITY (made with openssl):");
        row(out, 14, "root", dn(root));
        row(out, 14, "", "self-signed " + yn(selfSigned(root)) + ", CA " + yn(root.getBasicConstraints() >= 0) + ", path length " + pathLength(root));
        row(out, 14, "intermediate", dn(inter));
        row(out, 14, "", "signed by the root " + yn(signedBy(inter, root)) + ", CA " + yn(inter.getBasicConstraints() >= 0) + ", path length " + pathLength(inter));
        row(out, 14, "both", "key usage " + keyUsage(root) + (keyUsage(root).equals(keyUsage(inter)) ? "" : " / " + keyUsage(inter)));
        row(out, 14, "", "the root signs only the intermediate;");
        row(out, 14, "", "the intermediate signs every certificate, CRL");
        row(out, 14, "", "and OCSP answer below");

        Instant now = Instant.now();
        Path key = pki.newKeyAndRequest("api");
        Pki.Issued api = pki.issue("api.bank.example", key, List.of("api.bank.example", "payments.bank.example"), now.minusSeconds(60),
                now.plus(30, ChronoUnit.DAYS), true);
        X509Certificate leaf = api.cert();
        head(out, "THE CERTIFICATE (issued by the intermediate, read in Java):");
        row(out, 10, "names", names(leaf));
        row(out, 10, "usage", keyUsage(leaf) + "; extended " + extendedKeyUsage(leaf) + "; CA " + yn(leaf.getBasicConstraints() >= 0));
        row(out, 10, "key", leaf.getPublicKey().getAlgorithm() + " " + ((ECPublicKey) leaf.getPublicKey()).getParams().getOrder().bitLength()
                + " bits, signed " + leaf.getSigAlgName());
        row(out, 10, "lifetime", Duration.between(leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant()).plusMinutes(1).toDays() + " days");
        row(out, 10, "pointers", "OCSP responder " + yn(leaf.getExtensionValue("1.3.6.1.5.5.7.1.1") != null)
                + ", CRL distribution point " + yn(leaf.getExtensionValue("2.5.29.31") != null));

        Instant t = Instant.now();
        List<ChainSteps.Step> steps = ChainSteps.check(leaf, List.of(inter), root, t);
        head(out, "HOW THE CHAIN IS CHECKED (by hand, now):");
        for (int i = 0; i < steps.size(); i++) {
            row(out, 44, (i + 1) + " " + steps.get(i).what(), yn(steps.get(i).ok()));
        }
        head(out, "THE CHECKS (by hand, then by the JDK):");
        row(out, 24, "the chain", String.format("%-12s%s", "by hand", "the JDK"));
        Object[][] cases = {{"with the intermediate", List.of(inter), t}, {"no intermediate", List.<X509Certificate>of(), t},
            {"31 days later", List.of(inter), t.plus(31, ChronoUnit.DAYS)}, {"a day before issue", List.of(inter), t.minus(1, ChronoUnit.DAYS)}};
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            List<X509Certificate> chain = (List<X509Certificate>) c[1];
            Instant when = (Instant) c[2];
            List<ChainSteps.Step> s = ChainSteps.check(leaf, chain, root, when);
            String hand = ChainSteps.accepted(s) ? "accepted" : "fails at " + ChainSteps.firstFailure(s);
            row(out, 24, (String) c[0], String.format("%-12s%s", hand, Chains.validate(leaf, chain, root, when, Chains.Source.NONE, Chains.Policy.HARD, List.of()).word()));
        }
        return api;
    }

    private static String dn(X509Certificate c) {
        return c.getSubjectX500Principal().getName().replace(",", ", ");
    }

    // ----------------------------------------------------------------------------------------------------------------------------------- TWO. OCSP

    private static void ocsp(StringBuilder out, Pki pki, int port) throws Exception {
        X509Certificate root = pki.root();
        List<X509Certificate> chain = List.of(pki.intermediate());
        Instant now = Instant.now();
        Pki.Issued fine = issueFor(pki, "ledger.bank.example", now);
        Pki.Issued stolen = issueFor(pki, "cards.bank.example", now);
        head(out, "WHEN THE STATUS CANNOT BE LEARNED, A CLIENT CAN:");
        row(out, 11, "hard fail", "refuse the certificate");
        row(out, 11, "soft fail", "accept it anyway");
        head(out, "OCSP: THE CLIENT ASKS THE RESPONDER (openssl ocsp):");
        row(out, 24, "certificate, responder", String.format("%-18s%s", "hard fail", "soft fail"));
        int softLetThrough = 0;
        try (Responder responder = new Responder(pki, port)) {
            ocspRow(out, "good, up", fine, chain, root);
            int seen = responder.requests();
            pki.revoke(stolen);
            responder.reload();
            ocspRow(out, "revoked, up", stolen, chain, root);
            responder.mode(Responder.Mode.DOWN);
            ocspRow(out, "good, down", fine, chain, root);
            softLetThrough += ocspRow(out, "revoked, down", stolen, chain, root);
            responder.mode(Responder.Mode.HANG);
            long start = System.nanoTime();
            softLetThrough += ocspRow(out, "revoked, hangs", stolen, chain, root);
            long millis = (System.nanoTime() - start) / 1_000_000;
            head(out, "THE RESULT:");
            row(out, 37, "requests for the 2 good checks", String.valueOf(seen));
            row(out, 37, "a hanging check waited about 1 s", yn(millis > 1800 && millis < 6000) + " (the JDK default is 15 s)");
            row(out, 37, "revoked, blocked, soft fail accepts", softLetThrough + " of 2");
        }
    }

    /** One row of the OCSP table; returns 1 when a revoked certificate was accepted by the soft-fail client. */
    private static int ocspRow(StringBuilder out, String label, Pki.Issued cert, List<X509Certificate> chain, X509Certificate root) {
        Verdict hard = Chains.validate(cert.cert(), chain, root, Instant.now(), Chains.Source.OCSP, Chains.Policy.HARD, List.of());
        Verdict soft = Chains.validate(cert.cert(), chain, root, Instant.now(), Chains.Source.OCSP, Chains.Policy.SOFT, List.of());
        row(out, 24, label, String.format("%-18s%s", brief(hard), brief(soft)));
        return label.startsWith("revoked") && soft.accepted() ? 1 : 0;
    }

    /** A verdict in two words for a table: accepted, refused: revoked, refused: unknown. */
    private static String brief(Verdict v) {
        if (v.accepted()) {
            return "accepted";
        }
        return "refused: " + (v.why().equals("revocation status unknown") ? "unknown" : v.why());
    }

    private static Pki.Issued issueFor(Pki pki, String name, Instant now) throws Exception {
        Path key = pki.newKeyAndRequest(name.substring(0, name.indexOf('.')));
        return pki.issue(name, key, List.of(name), now.minusSeconds(60), now.plus(30, ChronoUnit.DAYS), true);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------ TWO. CRL

    private static void crl(StringBuilder out, Pki pki, Pki.Issued api) throws Exception {
        X509Certificate root = pki.root();
        X509Certificate inter = pki.intermediate();
        X509Certificate leaf = api.cert();
        List<X509Certificate> chain = List.of(inter);
        X509CRL before = pki.generateCrl(3600);
        before.verify(inter.getPublicKey());
        int beforeEntries = before.getRevokedCertificates() == null ? 0 : before.getRevokedCertificates().size();
        head(out, "A CRL: THE CLIENT DOWNLOADS A SIGNED LIST (good for 3600 s):");
        row(out, 15, "the CRL", beforeEntries + " entry, verified with the intermediate's key");
        pki.revoke(api);
        row(out, 15, "revoked after", "api.bank.example; a client with that CRL: "
                + Chains.validate(leaf, chain, root, Instant.now(), Chains.Source.CRL, Chains.Policy.HARD, List.of(before)).word());
        X509CRL after = pki.generateCrl(3600);
        after.verify(inter.getPublicKey());
        row(out, 15, "the next CRL", after.getRevokedCertificates().size() + " entries; a client with it: "
                + Chains.validate(leaf, chain, root, Instant.now(), Chains.Source.CRL, Chains.Policy.HARD, List.of(after)).word());
        Instant later = Instant.now().plus(2, ChronoUnit.HOURS);
        row(out, 15, "2 hours later", "next update passed: hard " + Chains.validate(leaf, chain, root, later, Chains.Source.CRL,
                Chains.Policy.HARD, List.of(after)).word().replace(" (revocation status unknown)", "") + ", soft "
                + Chains.validate(leaf, chain, root, later, Chains.Source.CRL, Chains.Policy.SOFT, List.of(after)).word());
        row(out, 15, "the gap", "a client with the old list accepts for up to 3600 s");
    }

    // ------------------------------------------------------------------------------------------------------------------------------- TWO. CRL size

    private static void crlSizes(StringBuilder out, Openssl ssl, Path dir) throws Exception {
        Pki pki = new Pki(ssl, dir, "http://127.0.0.1:1");
        Instant now = Instant.now();
        int empty = crlSize(pki, 0, 0, now, false).getEncoded().length;
        head(out, "HOW BIG A CRL GETS (openssl ca writes it):");
        row(out, 10, "entries", String.format("%-10s%-12s%s", "size", "per entry", "at 1 Mbit/s"));
        X509CRL small = null;
        X509CRL large = null;
        for (int n : new int[] {1_000, 10_000, 100_000}) {
            X509CRL crl = crlSize(pki, n, 0, now, false);
            int size = crl.getEncoded().length;
            small = small == null ? crl : small;
            large = crl;
            row(out, 10, String.format("%,d", n), String.format("%-10s%-12s%s", size / 1024 + " KB", Math.round((size - empty) / (double) n) + " bytes",
                    String.format("%.1f s", size * 8 / 1_000_000.0)));
        }
        X509CRLEntry first = small.getRevokedCertificates().iterator().next();
        X509CRLEntry last = large.getRevokedCertificates().stream().max(Comparator.comparing(X509CRLEntry::getSerialNumber)).orElseThrow();
        head(out, "HOW ONE ENTRY IS MADE (DER, read from the CRL):");
        entry(out, first);
        entry(out, last);
        row(out, 14, "time", "size x 8 bits / 1,000,000 bits a second");
        int marked = crlSize(pki, 100_000, 90_000, now, true).getEncoded().length;
        int dropped = crlSize(pki, 10_000, 0, now, false).getEncoded().length;
        head(out, "100,000 REVOKED, 90,000 OF THEM ALREADY EXPIRED:");
        row(out, 29, "openssl ca -updatedb keeps", marked / 1024 + " KB");
        row(out, 29, "dropped, as RFC 5280 allows", dropped / 1024 + " KB: short lives, short lists");
    }

    private static void entry(StringBuilder out, X509CRLEntry e) throws GeneralSecurityException {
        int serial = e.getSerialNumber().toByteArray().length;
        int total = e.getEncoded().length;
        row(out, 14, "serial " + e.getSerialNumber().toString(16).toUpperCase(), "2 + (2 + " + serial + " serial) + (2 + 13 date) = " + total + " bytes");
    }

    private static X509CRL crlSize(Pki pki, int revoked, int expired, Instant now, boolean prune) throws Exception {
        Files.writeString(pki.indexFile(), Crls.index(revoked, expired, now));
        if (prune) {
            pki.updateDatabase();
        }
        return pki.generateCrl(3600);
    }

    // ------------------------------------------------------------------------------------------------------------------------------- THREE. stapling

    private static void stapling(StringBuilder out, Openssl ssl, Pki pki, int port) throws Exception {
        X509Certificate root = pki.root();
        Instant now = Instant.now();
        Pki.Issued shop = issueFor(pki, "shop.bank.example", now);
        Path staple = pki.ocspResponseFor(shop, "staple-1");
        act(out, "THREE", "Stapling: the server brings the answer.");
        head(out, "THE STAPLE (openssl s_server -status_file):");
        row(out, 10, "what", "the OCSP answer, signed and dated, in the handshake");
        row(out, 10, "size", "under 512 bytes: " + yn(Files.size(staple) < 512));
        int w = 20;
        try (Responder responder = new Responder(pki, port)) {
            try (StaplingServer plain = new StaplingServer(ssl, pki, shop, null);
                    StaplingServer stapled = new StaplingServer(ssl, pki, shop, staple)) {
                head(out, "THE RESPONDER UP (a hard-fail client):");
                row(out, w, "server", String.format("%-10s%-14s%s", "verdict", "in handshake", "to responder"));
                responder.resetCount();
                StapleClient viaStaple = new StapleClient(root, Chains.Policy.HARD, true, Instant.now());
                StapleClient.Outcome o = viaStaple.connect(stapled.port());
                row(out, w, "staples", String.format("%-10s%-14s%s", o.verdict().word(), o.stapledResponses(), responder.requests()));
                responder.resetCount();
                o = viaStaple.connect(plain.port());
                row(out, w, "does not staple", String.format("%-10s%-14s%s", o.verdict().word(), o.stapledResponses(), responder.requests()));
                row(out, w, "", "and the responder sees which site");
                responder.mode(Responder.Mode.DOWN);
                head(out, "THE RESPONDER DOWN:");
                o = viaStaple.connect(plain.port());
                row(out, w, "does not staple", "hard fail " + brief(o.verdict()) + ", soft fail "
                        + brief(new StapleClient(root, Chains.Policy.SOFT, true, Instant.now()).connect(plain.port()).verdict()));
                o = viaStaple.connect(stapled.port());
                row(out, w, "staples", "hard fail " + brief(o.verdict()));
                o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now().plus(2, ChronoUnit.DAYS)).connect(stapled.port());
                row(out, w, "staples, 2 days on", "past next update: hard " + brief(o.verdict()).replace("OCSP response ", ""));
                responder.mode(Responder.Mode.ANSWER);
                pki.revoke(shop);
                responder.reload();
                responder.resetCount();
                head(out, "THE CERTIFICATE IS REVOKED (the responder up):");
                o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now()).connect(stapled.port());
                row(out, 30, "the old staple", brief(o.verdict()) + ", " + responder.requests() + " requests to it");
                o = new StapleClient(root, Chains.Policy.HARD, false, Instant.now()).connect(stapled.port());
                row(out, 30, "a client asking the responder", brief(o.verdict()));
            }
            Path fresh = pki.ocspResponseFor(shop, "staple-2");
            try (StaplingServer refreshed = new StaplingServer(ssl, pki, shop, fresh)) {
                StapleClient.Outcome o = new StapleClient(root, Chains.Policy.HARD, true, Instant.now()).connect(refreshed.port());
                row(out, 30, "a staple made after it", brief(o.verdict()));
            }
        }
        row(out, 30, "so a staple is as old as", "its last refresh (1 day here)");
    }

    // ---------------------------------------------------------------------------------------------------------------------------- FOUR. step-ca

    private static void stepCa(StringBuilder out, Path dir) throws Exception {
        act(out, "FOUR", "Short lives and automatic rotation.");
        try (StepCa ca = new StepCa(dir)) {
            X509Certificate caRoot = ca.rootCertificate();
            Credential first = ca.issue("svc0", "ledger.bank.example", LIFE);
            long life = Duration.between(first.leaf().getNotBefore().toInstant(), first.leaf().getNotAfter().toInstant()).toSeconds();
            head(out, "THE CERTIFICATE (step-ca, ledger.bank.example):");
            row(out, 12, "lifetime", LIFE + " seconds (as issued, within a second: " + yn(Math.abs(life - LIFE) <= 1) + ")");
            row(out, 12, "chain sent", first.chain().size() + " certificates");
            Instant t0 = Instant.EPOCH;
            Instant due = Renewer.renewAt(t0, t0.plusSeconds(LIFE));
            head(out, "HOW THE SCHEDULE IS MADE:");
            row(out, 12, "renew at", LIFE + " x 2/3 = " + Duration.between(t0, due).toSeconds() + " seconds after issue");
            row(out, 12, "headroom", LIFE + " - " + Duration.between(t0, due).toSeconds() + " = " + (LIFE - Duration.between(t0, due).toSeconds())
                    + " seconds for the CA to be away");
            row(out, 12, "alert at", "a quarter of the life left: " + LIFE + " / 4 = " + LIFE / 4 + " seconds");
            row(out, 12, "if stolen", "worth " + LIFE + " seconds at most, not 30 days");

            try (Service service = new Service(first); Renewer renewer = new Renewer(ca, service, "svc0"); ExpiryWatch watch = new ExpiryWatch(service.port())) {
                Load load = new Load(caRoot, "ledger.bank.example", service.port());
                Load.Tally tally = load.run(300, 100);
                head(out, "ROTATION (30 seconds, a handshake every 100 ms):");
                row(out, 12, "handshakes", tally.attempts() + ", failed " + tally.refused());
                row(out, 12, "renewals", renewer.renewed() + ", failed " + renewer.failed());
                row(out, 12, "alerts", String.valueOf(watch.firstAlert() == null ? 0 : 1));
                row(out, 12, "serial", "not the one at the start: "
                        + yn(!service.credential().leaf().getSerialNumber().equals(first.leaf().getSerialNumber())));
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
            head(out, "THE CA AWAY FOR ABOUT 2 S, JUST BEFORE A RENEWAL:");
            row(out, 12, "handshakes", tally.attempts() + " in 20 seconds, failed " + tally.refused());
            row(out, 12, "renewals", "failed while away: " + yn(renewer.failed() > 0) + ", done once back: " + yn(renewer.renewed() > 0));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------- FIVE. missed

    /** The CA stays away past the end of the certificate: nobody renews, and the service stops answering. */
    private static void missed(StringBuilder out, StepCa ca, X509Certificate caRoot) throws Exception {
        Credential cred = ca.issue("miss0", "ledger.bank.example", LIFE);
        act(out, "FIVE", "The missed renewal, and a monitor.");
        head(out, "THE CA STAYS DOWN PAST THE EXPIRY:");
        int w = 20;
        try (Service service = new Service(cred); Renewer renewer = new Renewer(ca, service, "miss0"); ExpiryWatch watch = new ExpiryWatch(service.port())) {
            ca.stop();
            Load load = new Load(caRoot, "ledger.bank.example", service.port());
            Load.Tally before = load.run(50, 100);
            row(out, w, "before the expiry", before.attempts() + " handshakes, " + before.refused() + " refused");
            long wait = Duration.between(Instant.now(), cred.leaf().getNotAfter().toInstant().plusSeconds(1)).toMillis();
            Thread.sleep(Math.max(0, wait));
            Load.Tally after = load.run(100, 100);
            row(out, w, "after the expiry", after.attempts() + " handshakes, " + after.refused() + " refused");
            row(out, w, "", "the reason: " + after.firstReason());
            row(out, w, "renewals", "failed, more than 10: " + yn(renewer.failed() > 10) + "; done " + renewer.renewed());
            Instant alert = watch.firstAlert();
            boolean early = alert != null && after.firstRefusal() != null && alert.isBefore(after.firstRefusal());
            row(out, w, "the monitor", "alerted before the first refusal: " + yn(early));
            row(out, w, "", "and at least 2 seconds before: " + yn(early && Duration.between(alert, after.firstRefusal()).toMillis() >= 2000));
            renewer.close();
            ca.start();
            Path crt = ca.file("miss0.crt");
            head(out, "THE CA IS BACK:");
            row(out, w, "renew the old one", "refused " + yn(!ca.tryRenew("miss0", "miss-again").ok()) + " (it has to be issued anew)");
            Credential reissued = ca.issue("miss1", "ledger.bank.example", LIFE);
            service.install(reissued);
            Load.Tally recovered = load.run(20, 100);
            row(out, w, "a new one", recovered.attempts() + " handshakes, " + recovered.refused() + " refused");
            if (!Files.exists(crt)) {
                throw new IllegalStateException("lost the certificate file");
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------- FIVE. fleet

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
        Map<Fleet.Bucket, Integer> counts = Fleet.count(entries, Instant.now());
        head(out, "AN INVENTORY OF " + entries.size() + " CERTIFICATES (the monitor's first scan):");
        for (Fleet.Bucket b : Fleet.Bucket.values()) {
            row(out, 18, b.label, String.valueOf(counts.get(b)));
        }
        row(out, 18, "", counts.get(Fleet.Bucket.EXPIRED) + " expired, found before any report");
        Instant now = Instant.now();
        Fleet.Model none = Fleet.model(entries, now, 60, 0, 0, false);
        head(out, "THE NEXT 60 DAYS (a scan every morning, " + none.expiringInHorizon() + " expire):");
        row(out, 30, "the monitor", String.format("%-10s%s", "outages", "prevented"));
        row(out, 30, "none", String.format("%-10s%s", none.outages(), none.prevented()));
        for (int[] cfg : new int[][] {{7, 3}, {7, 10}, {30, 10}}) {
            Fleet.Model m = Fleet.model(entries, now, 60, cfg[0], cfg[1], true);
            row(out, 30, "alert " + cfg[0] + " d, owner needs " + cfg[1] + " d", String.format("%-10s%s", m.outages(), m.prevented()));
        }
        head(out, "HOW AN OUTAGE IS COUNTED:");
        row(out, 10, "alert", "the first morning with fewer than the alert's days left");
        row(out, 10, "renewed", "the alert day + the days the owner needs");
        row(out, 10, "outage", "renewed after the expiry: a 7 day alert cannot help");
        row(out, 10, "", "a team that needs 10 days");
    }

    // ----------------------------------------------------------------------------------------------------------------------------------- helpers

    private static void act(StringBuilder out, String number, String title) {
        out.append(number).append(". ").append(title).append('\n');
    }

    private static void head(StringBuilder out, String text) {
        out.append("  ").append(text).append('\n');
    }

    private static void row(StringBuilder out, int width, String label, String value) {
        out.append(("    " + String.format("%-" + width + "s", label) + value).stripTrailing()).append('\n');
    }

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
