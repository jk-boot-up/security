package com.jk.explore.security.localca;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.x509.CRLReason;

/** Runs the online store's internal certificate authority in five numbered acts and prints exact, repeatable results. */
public class LocalCaDemo {

    static final Instant START = Instant.parse("2026-01-05T09:00:00Z");
    static final Map<String, Set<String>> ALLOWED = Map.of(
            "web-team", Set.of("www.shop.internal"),
            "payments-team", Set.of("payments.shop.internal", "pay-api.shop.internal"),
            "orders-team", Set.of("orders.shop.internal"),
            "cart-team", Set.of("cart.shop.internal"));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /**
     * The whole demo as text, so the tests can compare it with the golden file. The clock is fixed and serials count up from 1000;
     * keys are random, so nothing that depends on a key's bytes is printed.
     */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        CertificateAuthority ca = new CertificateAuthority(START, ALLOWED);
        Validator clients = new Validator(ca.root());
        X509Certificate www = act1RunningACa(out, ca, clients);
        X509Certificate payments = act2SigningRequests(out, ca, clients);
        act3Revocation(out, ca, clients, www, payments);
        act4ShortLived(out, ca, clients, www);
        act5TheBill(out, ca, clients, www);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static Instant day(int n) {
        return START.plus(Duration.ofDays(n));
    }

    private static long dayOf(java.util.Date date) {
        return Duration.between(START, date.toInstant()).toDays();
    }

    private static String date(java.util.Date date) {
        return DATE.format(date.toInstant());
    }

    private static String valid(X509Certificate certificate) {
        return date(certificate.getNotBefore()) + " to " + date(certificate.getNotAfter());
    }

    private static List<X509Certificate> chain(X509Certificate leaf, CertificateAuthority ca) {
        return List.of(leaf, ca.issuing());
    }

    private static String refusal(ThrowingIssue issue) {
        try {
            issue.run();
            return "issued";
        } catch (RefusedException e) {
            return "refused: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface ThrowingIssue {
        void run() throws RefusedException;
    }

    private static String purposes(X509Certificate certificate) throws CertificateParsingException {
        List<String> names = new ArrayList<>();
        for (String oid : certificate.getExtendedKeyUsage()) {
            names.add(switch (oid) {
                case "1.3.6.1.5.5.7.3.1" -> "serverAuth";
                case "1.3.6.1.5.5.7.3.2" -> "clientAuth";
                default -> oid;
            });
        }
        return String.join(", ", names);
    }

    private static String reason(X509CRLEntry entry) {
        return entry.getRevocationReason().name().toLowerCase(Locale.ROOT).replace('_', ' ').replace("ca ", "CA ");
    }

    private static void chainCheck(StringBuilder out, X509Certificate leaf, CertificateAuthority ca, Validator clients, Instant when, boolean allSteps) {
        List<ChainSteps.Step> steps = ChainSteps.walk(leaf, ca.issuing(), ca.root(), when);
        for (int i = 0; i < steps.size(); i++) {
            ChainSteps.Step step = steps.get(i);
            if (allSteps || !step.ok()) {
                line(out, "    " + (i + 1) + ". " + step.text() + ": " + step.ok());
            }
        }
        line(out, "  THE RESULT:");
        line(out, "    by hand:             " + ChainSteps.verdict(steps));
        line(out, "    the JDK's validator: " + clients.check(chain(leaf, ca), when));
    }

    private static X509Certificate act1RunningACa(StringBuilder out, CertificateAuthority ca, Validator clients) throws Exception {
        line(out, "ONE. Running a certificate authority for the store's own services.");
        X509Certificate root = ca.root();
        line(out, "  THE ROOT (signs its own certificate, then goes offline):");
        line(out, "    subject      " + ChainSteps.cn(root) + ", serial " + root.getSerialNumber());
        line(out, "    valid        " + valid(root));
        line(out, "    may certify  " + root.getBasicConstraints() + " level of authority below it");
        X509Certificate issuing = ca.issuing();
        line(out, "  THE ISSUING AUTHORITY (signed by the root):");
        line(out, "    subject      " + ChainSteps.cn(issuing) + ", serial " + issuing.getSerialNumber());
        line(out, "    valid        " + valid(issuing));
        line(out, "    path length  " + issuing.getBasicConstraints() + ": it may not create authorities");
        line(out, "    names        only under " + ChainSteps.permittedDomain(issuing));
        line(out, "  THE REQUEST (web-team):");
        line(out, "    www.shop.internal, 90 days, server profile");
        X509Certificate www = ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal")), "web-team", Profile.SERVER, Duration.ofDays(90), day(0));
        boolean[] usage = www.getKeyUsage();
        line(out, "  THE CERTIFICATE:");
        line(out, "    serial       " + www.getSerialNumber() + ", issued by " + ChainSteps.cn(www.getIssuerX500Principal().getName()));
        line(out, "    names        " + String.join(", ", ChainSteps.names(www)));
        line(out, "    valid        " + valid(www) + " ("
                + CertificateAuthority.describe(Duration.between(www.getNotBefore().toInstant(), www.getNotAfter().toInstant())) + ")");
        line(out, "    purpose      " + purposes(www) + ", digital signature only: " + (usage[0] && !usage[5]));
        line(out, "    authority    " + (www.getBasicConstraints() == -1 ? "no: it may not sign certificates" : "yes"));
        line(out, "  THE CHAIN CHECK BY HAND (what a client does, on " + DATE.format(day(1)) + "):");
        chainCheck(out, www, ca, clients, day(1), true);
        line(out, "  THE REQUESTS THE POLICY REFUSES:");
        line(out, "    web-team, no subject alternative name");
        line(out, "      -> " + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of()), "web-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        line(out, "    web-team, www.shop.internal for 400 days");
        line(out, "      -> " + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal")), "web-team", Profile.SERVER,
                Duration.ofDays(400), day(0))));
        line(out, "    payments-team, pay.example");
        line(out, "      -> " + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("pay.example")), "payments-team", Profile.SERVER,
                Duration.ofDays(90), day(0))));
        X509Certificate stray = ca.issueWithoutPolicy(Keys.newPair().getPublic(), "pay.example", day(0));
        line(out, "  THE POLICY SKIPPED (a buggy or stolen issuer):");
        line(out, "    pay.example signed anyway, serial " + stray.getSerialNumber());
        line(out, "  THE CLIENT'S CHECK OF IT:");
        chainCheck(out, stray, ca, clients, day(1), false);
        line(out, "    <- THE NAME CONSTRAINT HOLDS");
        return www;
    }

    private static X509Certificate act2SigningRequests(StringBuilder out, CertificateAuthority ca, Validator clients) throws Exception {
        line(out, "TWO. Issuing from a signing request.");
        KeyPair paymentsKeys = Keys.newPair();
        var request = Csr.create(paymentsKeys, List.of("payments.shop.internal"));
        PublicKey carried = Csr.publicKey(request);
        line(out, "  THE REQUEST (payments-team signs it with its own private key):");
        line(out, "    names        " + String.join(", ", Csr.names(request)));
        line(out, "    public key   " + carried.getAlgorithm() + ", " + ((ECPublicKey) carried).getParams().getCurve().getField().getFieldSize()
                + "-bit curve, inside the request");
        line(out, "    private key  stays with payments-team: not in the request");
        line(out, "  THE AUTHORITY'S CHECKS, IN ORDER:");
        line(out, "    1. the signature verifies with the request's key: " + Csr.provesPossession(request));
        line(out, "    2. at least one name: " + !Csr.names(request).isEmpty());
        line(out, "    3. payments.shop.internal is inside " + CertificateAuthority.DOMAIN + ": "
                + "payments.shop.internal".endsWith("." + CertificateAuthority.DOMAIN));
        line(out, "    4. payments-team may ask for it: " + ALLOWED.get("payments-team").contains("payments.shop.internal"));
        line(out, "    5. 90 days is within the server profile's "
                + CertificateAuthority.describe(Profile.SERVER.maxLifetime()) + ": " + (Duration.ofDays(90).compareTo(Profile.SERVER.maxLifetime()) <= 0));
        X509Certificate payments = ca.issue(request, "payments-team", Profile.SERVER, Duration.ofDays(90), day(0));
        line(out, "  THE CERTIFICATE:");
        line(out, "    serial " + payments.getSerialNumber() + "; its public key is the request's: " + payments.getPublicKey().equals(paymentsKeys.getPublic()));
        KeyPair stranger = Keys.newPair();
        line(out, "  THE FORGED REQUEST (payments-team's key, another key signs):");
        line(out, "    -> " + refusal(() -> ca.issue(Csr.signedByAnotherKey(paymentsKeys.getPublic(), stranger.getPrivate(),
                List.of("payments.shop.internal")), "payments-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        line(out, "  THE WRONG TEAM (web-team asks):");
        line(out, "    payments.shop.internal");
        line(out, "      -> " + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("payments.shop.internal")), "web-team", Profile.SERVER,
                Duration.ofDays(90), day(0))));
        line(out, "    www.shop.internal and payments.shop.internal at once");
        line(out, "      -> " + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal", "payments.shop.internal")), "web-team",
                Profile.SERVER, Duration.ofDays(90), day(0))));
        X509Certificate client = ca.issue(Csr.create(Keys.newPair(), List.of("orders.shop.internal")), "orders-team", Profile.CLIENT, Duration.ofDays(30), day(0));
        line(out, "  THE CLIENT CERTIFICATE (orders-team calls other services):");
        line(out, "    serial " + client.getSerialNumber() + ", " + String.join(", ", ChainSteps.names(client)) + ", " + purposes(client) + ", "
                + CertificateAuthority.describe(Duration.between(client.getNotBefore().toInstant(), client.getNotAfter().toInstant())));
        line(out, "    the chain check: " + clients.check(chain(client, ca), day(1)));
        return payments;
    }

    private static void printList(StringBuilder out, String title, X509CRL list) {
        line(out, title);
        line(out, "    this update  " + date(list.getThisUpdate()) + " (day " + dayOf(list.getThisUpdate()) + ")");
        line(out, "    next update  " + date(list.getNextUpdate()) + " (day " + dayOf(list.getNextUpdate()) + ")");
        Set<? extends X509CRLEntry> entries = list.getRevokedCertificates();
        if (entries == null) {
            line(out, "    revoked      none");
        } else {
            for (X509CRLEntry entry : entries) {
                line(out, "    revoked      serial " + entry.getSerialNumber() + ", " + reason(entry));
            }
        }
    }

    private static void act3Revocation(StringBuilder out, CertificateAuthority ca, Validator clients, X509Certificate www, X509Certificate payments) {
        line(out, "THREE. Revocation: taking a certificate back before it expires.");
        X509CRL rootList = ca.rootCrl(day(0), Duration.ofDays(365), false);
        ca.revoke(payments.getSerialNumber(), CRLReason.keyCompromise, day(10));
        X509CRL list = ca.issuingCrl(day(10), Duration.ofDays(7));
        line(out, "  THE REPORT (day 10): payments.shop.internal's key is stolen");
        printList(out, "  THE ISSUING CA'S REVOCATION LIST:", list);
        printList(out, "  THE ROOT'S REVOCATION LIST (covers the issuing CA):", rootList);
        line(out, "  THE CHECKS ON DAY 11:");
        line(out, "    payments.shop.internal, the client checks the lists");
        line(out, "      -> " + clients.checkWithLists(chain(payments, ca), day(11), List.of(list, rootList)));
        line(out, "    www.shop.internal, the client checks the lists");
        line(out, "      -> " + clients.checkWithLists(chain(www, ca), day(11), List.of(list, rootList)));
        line(out, "    payments.shop.internal, a client that never checks");
        line(out, "      -> " + clients.check(chain(payments, ca), day(11)) + "  <- A REVOKED CERTIFICATE STILL WORKS");
        line(out, "  THE CHECKS ON DAY 20 (the day-10 list ran out on day " + dayOf(list.getNextUpdate()) + "):");
        line(out, "    www.shop.internal, only the day-10 list");
        line(out, "      -> " + clients.checkWithLists(chain(www, ca), day(20), List.of(list, rootList)));
        line(out, "      <- NO CURRENT LIST, SO NO: IT FAILS CLOSED");
        X509CRL fresh = ca.issuingCrl(day(20), Duration.ofDays(7));
        line(out, "    www.shop.internal, a fresh list from day " + dayOf(fresh.getThisUpdate()));
        line(out, "      -> " + clients.checkWithLists(chain(www, ca), day(20), List.of(fresh, rootList)));
    }

    private static void act4ShortLived(StringBuilder out, CertificateAuthority ca, Validator clients, X509Certificate www) throws Exception {
        line(out, "FOUR. Short-lived certificates instead of revocation.");
        ShortLived cart = new ShortLived(ca, "cart-team", "cart.shop.internal");
        Instant begin = day(30);
        int refusedChecks = 0;
        for (int hour = 0; hour < 72; hour++) {
            Instant now = begin.plus(Duration.ofHours(hour));
            cart.tick(now);
            if (!clients.check(chain(cart.current(), ca), now).accepted()) {
                refusedChecks++;
            }
        }
        List<X509Certificate> history = cart.history();
        X509Certificate first = history.get(0);
        Duration life = Duration.between(first.getNotBefore().toInstant(), first.getNotAfter().toInstant());
        line(out, "  THE FIRST CERTIFICATE (cart.shop.internal, day " + dayOf(first.getNotBefore()) + "):");
        line(out, "    serial " + first.getSerialNumber() + ", valid " + valid(first));
        line(out, "  HOW THE RENEWAL TIME IS WORKED OUT:");
        line(out, "    lifetime     " + CertificateAuthority.describe(life) + ", from notBefore to notAfter");
        line(out, "    two thirds   " + life.toHours() + " x 2 / 3 = " + life.multipliedBy(2).dividedBy(3).toHours() + " hours");
        line(out, "    renew at     " + date(first.getNotBefore()) + " + " + life.multipliedBy(2).dividedBy(3).toHours() + " hours = "
                + DATE.format(ShortLived.renewAt(first)));
        List<String> hours = new ArrayList<>();
        Set<PublicKey> keys = new HashSet<>();
        for (X509Certificate certificate : history) {
            keys.add(certificate.getPublicKey());
            if (certificate != first) {
                hours.add(String.valueOf(Duration.between(begin, certificate.getNotBefore().toInstant()).toHours()));
            }
        }
        line(out, "  THE 72 HOURS, CHECKED EVERY HOUR:");
        line(out, "    renewed at hours " + String.join(", ", hours) + ": " + cart.renewals() + " renewals");
        line(out, "    " + history.size() + " certificates, " + keys.size() + " different keys; failed checks: " + refusedChecks);
        X509Certificate last = cart.current();
        Instant lastIssued = last.getNotBefore().toInstant();
        cart.stopJob();
        Instant later = lastIssued.plus(Duration.ofHours(25));
        cart.tick(later);
        line(out, "  THE RENEWAL JOB STOPS:");
        line(out, "    the last certificate, serial " + last.getSerialNumber() + ", ends " + date(last.getNotAfter()));
        line(out, "    25 hours after it began: " + clients.check(chain(cart.current(), ca), later));
        X509Certificate orders = ca.issue(Csr.create(Keys.newPair(), List.of("orders.shop.internal")), "orders-team", Profile.SERVER, Duration.ofDays(90), day(0));
        line(out, "  THE INVENTORY ON DAY 65 (renew after two thirds of a life):");
        for (Inventory.Entry entry : Inventory.report(List.of(www, orders, cart.current()), day(65))) {
            line(out, "    " + String.format("%-22s %-10s %s left", entry.name(), entry.status(), entry.left()));
        }
        line(out, "  A STOLEN KEY WORKS FOR:");
        line(out, "    a 24-hour certificate: at most 24 hours, with no list at all");
        line(out, "    a 90-day certificate: up to 90 days, unless every client checks");
    }

    private static void act5TheBill(StringBuilder out, CertificateAuthority ca, Validator clients, X509Certificate www) {
        line(out, "FIVE. The bill.");
        X509CRL compromised = ca.rootCrl(day(40), Duration.ofDays(365), true);
        X509CRL list = ca.issuingCrl(day(40), Duration.ofDays(7));
        line(out, "  THE ROOT KEY'S SIGNATURES (each one a ceremony, offline):");
        List<String> uses = ca.rootUses();
        for (int i = 0; i < uses.size(); i++) {
            line(out, "    " + (i + 1) + ". " + uses.get(i));
        }
        X509CRLEntry entry = compromised.getRevokedCertificates().iterator().next();
        line(out, "  THE ROOT'S LIST OF DAY " + dayOf(compromised.getThisUpdate()) + " (the issuing CA's key is stolen):");
        line(out, "    revoked      serial " + entry.getSerialNumber() + " (" + ChainSteps.cn(ca.issuing()) + "), " + reason(entry));
        line(out, "  THE CHECK ON DAY 41:");
        line(out, "    www.shop.internal -> " + clients.checkWithLists(chain(www, ca), day(41), List.of(list, compromised)));
        line(out, "    and so is every certificate below the issuing CA");
        List<String> audit = ca.audit();
        List<String> refusals = audit.stream().filter(e -> e.startsWith("refused")).toList();
        line(out, "  THE AUDIT LOG (" + audit.size() + " decisions, " + refusals.size() + " of them refusals):");
        for (String refusal : refusals) {
            line(out, "    " + refusal);
        }
        line(out, "  STILL TO AUTOMATE, PER SERVICE:");
        line(out, "    a request, a renewal job and a deployment step: ACME does it,");
        line(out, "    as the -with-step-ca version shows");
    }
}
