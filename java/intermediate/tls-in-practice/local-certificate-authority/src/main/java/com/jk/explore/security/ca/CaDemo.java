package com.jk.explore.security.ca;

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Runs the local certificate authority story in five numbered acts and prints exact, repeatable results. Keys are random and never printed. */
public class CaDemo {

    static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    static final List<String> NAMES = List.of("shop.example", "*.shop.example");

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        LocalCa ca = new LocalCa("Demo Root CA", NOW);
        Verifier client = new Verifier(ca.rootCertificate());
        KeyPair shopKey = LocalCa.newKey();

        out.append("== 1. Issue a certificate and trust the root ==\n");
        X509Certificate cert = ca.issue("shop.example", NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        X509CRL emptyList = ca.publishCrl(NOW, Duration.ofDays(1));
        out.append("root: subject=").append(ca.rootCertificate().getSubjectX500Principal()).append(", self-signed, valid ")
                .append(days(ca.rootCertificate().getNotBefore().toInstant(), ca.rootCertificate().getNotAfter().toInstant())).append(" days\n");
        out.append("leaf: subject=").append(cert.getSubjectX500Principal()).append(", issuer=").append(cert.getIssuerX500Principal())
                .append(", serial ").append(cert.getSerialNumber()).append(", valid ").append(days(cert.getNotBefore().toInstant(), cert.getNotAfter().toInstant())).append(" days\n");
        out.append("names in the certificate -> ").append(NAMES).append('\n');
        out.append(line("connect to shop.example", client, List.of(cert), "shop.example", List.of(emptyList), NOW));

        out.append("\n== 2. What a client refuses ==\n");
        out.append(line("connect to www.shop.example (wildcard)", client, List.of(cert), "www.shop.example", List.of(emptyList), NOW));
        out.append(line("connect to a.b.shop.example", client, List.of(cert), "a.b.shop.example", List.of(emptyList), NOW));
        out.append(line("connect to evil.example", client, List.of(cert), "evil.example", List.of(emptyList), NOW));
        out.append(line("100 days later (expired)", client, List.of(cert), "shop.example", List.of(ca.publishCrl(NOW.plus(Duration.ofDays(100)), Duration.ofDays(1))), NOW.plus(Duration.ofDays(100))));
        LocalCa other = new LocalCa("Other CA", NOW);
        X509Certificate fromOther = other.issue("shop.example", NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        out.append(line("issued by another authority", client, List.of(fromOther), "shop.example", List.of(emptyList), NOW));
        X509Certificate forged = ca.forge("shop.example", NAMES, shopKey.getPublic(), LocalCa.newKey(), NOW);
        out.append(line("claims our CA, signed by another key", client, List.of(forged), "shop.example", List.of(emptyList), NOW));
        X509Certificate selfSigned = Certificates.build(new Certificates.Spec("shop.example", "shop.example", java.math.BigInteger.TEN, NOW.minusSeconds(60), NOW.plus(Duration.ofDays(30)), false, NAMES),
                shopKey.getPublic(), shopKey.getPrivate());
        out.append(line("a self-signed server certificate", client, List.of(selfSigned), "shop.example", List.of(emptyList), NOW));
        KeyPair victimKey = LocalCa.newKey();
        X509Certificate underLeaf = Certificates.build(new Certificates.Spec("bank.example", "shop.example", java.math.BigInteger.valueOf(7), NOW.minusSeconds(60), NOW.plus(Duration.ofDays(30)), false,
                List.of("bank.example")), victimKey.getPublic(), shopKey.getPrivate());
        out.append(line("a leaf that signs another certificate", client, List.of(underLeaf, cert), "bank.example", List.of(emptyList), NOW));

        out.append("\n== 3. Revocation ==\n");
        Instant stolen = NOW.plus(Duration.ofDays(10));
        ca.revoke(cert, stolen);
        X509CRL revokedList = ca.publishCrl(stolen, Duration.ofDays(1));
        out.append("the key is stolen on day 10; the CA revokes serial ").append(cert.getSerialNumber()).append(" and publishes list number 2\n");
        out.append(line("with the new list", client, List.of(cert), "shop.example", List.of(revokedList), stolen.plusSeconds(60)));
        out.append(line("with the list from day 0 (a stale one)", client, List.of(cert), "shop.example", List.of(emptyList), stolen.plusSeconds(60)));
        out.append(line("with no list at all", client, List.of(cert), "shop.example", List.of(), stolen.plusSeconds(60)));
        out.append(line("with the new list, but it is 2 days old", client, List.of(cert), "shop.example", List.of(revokedList), stolen.plus(Duration.ofDays(2))));

        out.append("\n== 4. Renewal ==\n");
        Instant later = NOW.plus(Duration.ofDays(80));
        LocalCa ca2 = new LocalCa("Demo Root CA", NOW);
        Verifier client2 = new Verifier(ca2.rootCertificate());
        X509Certificate first = ca2.issue("shop.example", NAMES, shopKey.getPublic(), NOW, Duration.ofDays(90));
        X509Certificate renewed = ca2.renew(first, NAMES, later, Duration.ofDays(90));
        out.append("serial ").append(first.getSerialNumber()).append(" renewed as serial ").append(renewed.getSerialNumber()).append(", same key: ")
                .append(first.getPublicKey().equals(renewed.getPublicKey())).append('\n');
        Instant day100 = NOW.plus(Duration.ofDays(100));
        X509CRL list100 = ca2.publishCrl(day100, Duration.ofDays(1));
        out.append(line("old certificate on day 100", client2, List.of(first), "shop.example", List.of(list100), day100));
        out.append(line("renewed certificate on day 100", client2, List.of(renewed), "shop.example", List.of(list100), day100));

        out.append("\n== 5. Short-lived certificates instead of revocation ==\n");
        Duration listEvery = Duration.ofDays(1);
        Duration shortLife = Duration.ofHours(8);
        out.append("revocation: the stolen key is usable until clients fetch a new list, up to ").append(listEvery.toHours()).append(" hours after the theft is noticed\n");
        out.append("8-hour certificates: the stolen key stops working after at most ").append(shortLife.toHours()).append(" hours, with no list to fetch\n");
        X509Certificate shortCert = ca.issue("shop.example", NAMES, shopKey.getPublic(), NOW, shortLife);
        X509CRL crlNow = ca.publishCrl(NOW, listEvery);
        out.append(line("short-lived certificate, now", client, List.of(shortCert), "shop.example", List.of(crlNow), NOW));
        out.append(line("short-lived certificate, 9 hours later", client, List.of(shortCert), "shop.example", List.of(ca.publishCrl(NOW.plus(Duration.ofHours(9)), listEvery)), NOW.plus(Duration.ofHours(9))));
        return out.toString();
    }

    private static long days(Instant a, Instant b) {
        return Duration.between(a, b).toDays();
    }

    private static String line(String label, Verifier v, List<X509Certificate> chain, String host, List<X509CRL> crls, Instant at) {
        String result;
        try {
            v.check(chain, host, crls, at);
            result = "accepted";
        } catch (Verifier.Untrusted e) {
            result = "refused: " + e.getMessage();
        }
        return String.format("%-42s -> %s%n", label, result);
    }
}
