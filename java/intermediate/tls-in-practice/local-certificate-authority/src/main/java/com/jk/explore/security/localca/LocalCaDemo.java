package com.jk.explore.security.localca;

import java.security.KeyPair;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys are random, so nothing key-dependent is printed. */
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

    private static String sans(X509Certificate certificate) throws CertificateParsingException {
        List<String> names = new ArrayList<>();
        for (List<?> entry : certificate.getSubjectAlternativeNames()) {
            names.add(String.valueOf(entry.get(1)));
        }
        return String.join(", ", names);
    }

    private static X509Certificate act1RunningACa(StringBuilder out, CertificateAuthority ca, Validator clients) throws Exception {
        line(out, "ONE. Running a certificate authority for the store's own services.");
        line(out, "  the root: " + ca.root().getSubjectX500Principal().getName() + ", may certify " + ca.root().getBasicConstraints()
                + " level of authority below it, kept offline");
        line(out, "  the issuing authority: path length " + ca.issuing().getBasicConstraints() + " (it may not create authorities), names limited to "
                + CertificateAuthority.DOMAIN);
        X509Certificate www = ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal")), "web-team", Profile.SERVER, Duration.ofDays(90), day(0));
        boolean[] usage = www.getKeyUsage();
        line(out, "  issued for web-team: " + sans(www) + ", serial " + www.getSerialNumber() + ", " + purposes(www)
                + ", digital signature only: " + (usage[0] && !usage[5]) + ", not an authority: " + (www.getBasicConstraints() == -1)
                + ", " + CertificateAuthority.describe(Duration.between(www.getNotBefore().toInstant(), www.getNotAfter().toInstant())));
        line(out, "  clients validate it against the root: " + clients.check(chain(www, ca), day(1)));
        line(out, "  a request with no subject alternative name: "
                + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of()), "web-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        line(out, "  a request for 400 days: "
                + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal")), "web-team", Profile.SERVER, Duration.ofDays(400), day(0))));
        line(out, "  a request for pay.example: "
                + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("pay.example")), "payments-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        X509Certificate stray = ca.issueWithoutPolicy(Keys.newPair().getPublic(), "pay.example", day(0));
        line(out, "  the same name signed with the policy skipped, as a buggy or stolen issuer would: clients say "
                + clients.check(chain(stray, ca), day(1)) + "  <- THE NAME CONSTRAINT HOLDS");
        return www;
    }

    private static X509Certificate act2SigningRequests(StringBuilder out, CertificateAuthority ca, Validator clients) throws Exception {
        line(out, "TWO. Issuing from a signing request.");
        KeyPair paymentsKeys = Keys.newPair();
        var request = Csr.create(paymentsKeys, List.of("payments.shop.internal"));
        line(out, "  payments-team signs a request for payments.shop.internal with its own key; the request carries only the public key");
        line(out, "  the signature proves the sender holds the private key: " + Csr.provesPossession(request));
        X509Certificate payments = ca.issue(request, "payments-team", Profile.SERVER, Duration.ofDays(90), day(0));
        line(out, "  issued serial " + payments.getSerialNumber() + "; its public key is the one in the request: "
                + payments.getPublicKey().equals(paymentsKeys.getPublic()) + "; the authority never saw the private key");
        KeyPair stranger = Keys.newPair();
        line(out, "  a request carrying payments-team's public key but signed by another key: "
                + refusal(() -> ca.issue(Csr.signedByAnotherKey(paymentsKeys.getPublic(), stranger.getPrivate(), List.of("payments.shop.internal")),
                        "payments-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        line(out, "  web-team asking for payments.shop.internal: "
                + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("payments.shop.internal")), "web-team", Profile.SERVER, Duration.ofDays(90), day(0))));
        line(out, "  web-team asking for www.shop.internal and payments.shop.internal at once: "
                + refusal(() -> ca.issue(Csr.create(Keys.newPair(), List.of("www.shop.internal", "payments.shop.internal")), "web-team", Profile.SERVER,
                        Duration.ofDays(90), day(0))));
        X509Certificate client = ca.issue(Csr.create(Keys.newPair(), List.of("orders.shop.internal")), "orders-team", Profile.CLIENT, Duration.ofDays(30), day(0));
        line(out, "  orders-team gets a client certificate for calling other services: " + purposes(client) + ", " + sans(client) + ", "
                + clients.check(chain(client, ca), day(1)));
        return payments;
    }

    private static void act3Revocation(StringBuilder out, CertificateAuthority ca, Validator clients, X509Certificate www, X509Certificate payments) {
        line(out, "THREE. Revocation: taking a certificate back before it expires.");
        X509CRL rootList = ca.rootCrl(day(0), Duration.ofDays(365), false);
        ca.revoke(payments.getSerialNumber(), CRLReason.keyCompromise, day(10));
        X509CRL list = ca.issuingCrl(day(10), Duration.ofDays(7));
        line(out, "  payments.shop.internal's key is reported stolen on day 10; the issuing authority lists serial " + payments.getSerialNumber()
                + " in a revocation list valid until day 17, and the root's own list covers the issuing authority");
        line(out, "  day 11, payments.shop.internal, with revocation checked: " + clients.checkWithLists(chain(payments, ca), day(11), List.of(list, rootList)));
        line(out, "  day 11, www.shop.internal, with revocation checked: " + clients.checkWithLists(chain(www, ca), day(11), List.of(list, rootList)));
        line(out, "  day 11, payments.shop.internal, by a client that never checks: " + clients.check(chain(payments, ca), day(11))
                + "  <- A REVOKED CERTIFICATE STILL WORKS THERE");
        line(out, "  day 20, www.shop.internal, with only the day-10 list, now past its next update: "
                + clients.checkWithLists(chain(www, ca), day(20), List.of(list, rootList)) + "  <- NO CURRENT LIST, SO NO: IT FAILS CLOSED");
        X509CRL fresh = ca.issuingCrl(day(20), Duration.ofDays(7));
        line(out, "  day 20, with a fresh list: " + clients.checkWithLists(chain(www, ca), day(20), List.of(fresh, rootList)));
        line(out, "  revocation only works if the authority publishes on time and every client fetches and checks");
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
        line(out, "  cart.shop.internal gets 24-hour certificates and renews at two thirds of the lifetime, with a new key each time");
        line(out, "  over 72 hours, checked every hour: " + cart.renewals() + " renewals, " + refusedChecks + " failed checks");
        Instant lastIssued = cart.current().getNotBefore().toInstant();
        cart.stopJob();
        Instant later = lastIssued.plus(Duration.ofHours(25));
        cart.tick(later);
        line(out, "  the renewal job stops; 25 hours after the last renewal the certificate is " + clients.check(chain(cart.current(), ca), later));
        line(out, "  a stolen 24-hour key works for at most 24 hours with no list at all; a stolen 90-day key works for up to 90 days unless every client checks a list");
        X509Certificate orders = ca.issue(Csr.create(Keys.newPair(), List.of("orders.shop.internal")), "orders-team", Profile.SERVER, Duration.ofDays(90), day(0));
        Instant when = day(65);
        line(out, "  the inventory on day 65:");
        List<X509Certificate> all = List.of(www, orders, cart.current());
        for (Inventory.Entry entry : Inventory.report(all, when)) {
            line(out, "    " + entry.name() + ": " + entry.status() + ", " + entry.left() + " left");
        }
    }

    private static void act5TheBill(StringBuilder out, CertificateAuthority ca, Validator clients, X509Certificate www) {
        line(out, "FIVE. The bill.");
        line(out, "  the root key signed " + ca.rootSignatures() + " times in all: the issuing authority, itself and its revocation lists; keep it offline, ideally in hardware");
        X509CRL compromised = ca.rootCrl(day(40), Duration.ofDays(365), true);
        X509CRL list = ca.issuingCrl(day(40), Duration.ofDays(7));
        line(out, "  if the issuing authority's key is stolen, the offline root revokes it: www.shop.internal is then "
                + clients.checkWithLists(chain(www, ca), day(41), List.of(list, compromised)) + ", and so is every certificate below it");
        List<String> audit = ca.audit();
        long refusals = audit.stream().filter(entry -> entry.startsWith("refused")).count();
        line(out, "  the audit log holds " + audit.size() + " decisions, " + refusals + " of them refusals; an authority is a high-value target, so every decision is recorded");
        line(out, "  every service needs a request, a renewal job and a deployment step: in practice this is automated with ACME, as the -with-step-ca version shows");
    }
}
