package com.jk.explore.security.certs;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Keys are random, so only facts that do not depend on them are printed. */
public class CertsDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        Pki pki = new Pki();
        act1Chain(out, pki);
        act2GoodChain(out, pki);
        act3Refusals(out, pki);
        act4Tampering(out, pki);
        act5Revocation(out, pki);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String name(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName();
    }

    private static ChainValidator trusting(X509Certificate root) {
        return new ChainValidator(Set.of(root), Set.of());
    }

    private static void act1Chain(StringBuilder out, Pki pki) {
        line(out, "ONE. A chain of three certificates.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        X509Certificate leaf = pki.server.certificate();
        line(out, "  root:         " + name(root) + ", signed by itself: " + root.getIssuerX500Principal().equals(root.getSubjectX500Principal()));
        line(out, "  intermediate: " + name(middle) + ", signed by " + middle.getIssuerX500Principal().getName() + ", may issue certificates: " + (middle.getBasicConstraints() >= 0));
        line(out, "  server:       " + name(leaf) + ", signed by " + leaf.getIssuerX500Principal().getName() + ", may issue certificates: " + (leaf.getBasicConstraints() >= 0));
        line(out, "  each certificate holds its owner's public key and the issuer's signature over it, both as plain data");
        line(out, "  the signature algorithm is " + leaf.getSigAlgName() + ", and anyone can check it with the issuer's public key");
    }

    private static void act2GoodChain(StringBuilder out, Pki pki) {
        line(out, "TWO. The browser's check.");
        ChainValidator browser = trusting(pki.root.certificate());
        ChainValidator.Result result = browser.validate(List.of(pki.server.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        line(out, "  the server sends its own certificate and the intermediate; the browser already trusts the root");
        line(out, "  the chain is accepted: " + result.accepted());
        line(out, "  the certificate is for shop.example: " + HostName.matches(pki.server.certificate(), "shop.example"));
        line(out, "  the certificate is for evil.example: " + HostName.matches(pki.server.certificate(), "evil.example"));
        line(out, "  a wildcard *.example stands for exactly one label: shop.example " + HostName.matchesPattern("*.example", "shop.example")
                + ", a.b.example " + HostName.matchesPattern("*.example", "a.b.example"));
    }

    private static void act3Refusals(StringBuilder out, Pki pki) {
        line(out, "THREE. Three reasons to refuse.");
        ChainValidator browser = trusting(pki.root.certificate());
        Issued expired = pki.expiredServer();
        ChainValidator.Result late = browser.validate(List.of(expired.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        line(out, "  a certificate that ended on 2024-12-31, checked on 2026-01-01: accepted " + late.accepted() + ", reason " + late.reason());
        Issued[] rogue = Pki.rogue();
        ChainValidator.Result foreign = browser.validate(List.of(rogue[1].certificate()), Pki.TODAY);
        line(out, "  a certificate for shop.example from an authority the browser does not trust: accepted " + foreign.accepted() + ", reason " + foreign.reason());
        line(out, "  the rogue root has the same name as the real one: " + name(rogue[0].certificate()).equals(name(pki.root.certificate())) + ", so names prove nothing, only keys do");
        ChainValidator.Result valid = browser.validate(List.of(pki.server.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        line(out, "  the same valid chain, shown to a visitor who meant evil.example: chain accepted " + valid.accepted() + ", name matches: " + HostName.matches(pki.server.certificate(), "evil.example"));
        line(out, "  so a client must check the chain and the name, and both are separate checks");
    }

    private static void act4Tampering(StringBuilder out, Pki pki) {
        line(out, "FOUR. Changed certificates, and certificates that overstep.");
        ChainValidator browser = trusting(pki.root.certificate());
        X509Certificate changed = Tamper.changeName(pki.server.certificate());
        ChainValidator.Result result = browser.validate(List.of(changed, pki.intermediate.certificate()), Pki.TODAY);
        line(out, "  one letter of the name changed (" + name(changed) + "): it still parses, accepted " + result.accepted() + ", reason " + result.reason());
        Issued forged = CertBuilder.issue(CertBuilder.Request.server("other.example", Pki.START, java.time.LocalDate.of(2026, 6, 1), 50), CertBuilder.newKeys(), pki.server);
        ChainValidator.Result overstep = browser.validate(List.of(forged.certificate(), pki.server.certificate(), pki.intermediate.certificate()), Pki.TODAY);
        line(out, "  the shop's server key signs a certificate for other.example: accepted " + overstep.accepted() + ", reason " + overstep.reason());
        line(out, "  a server certificate says it is not a CA, so what it signs is not trusted");
        line(out, "  the intermediate's limit on further authorities below it (path length): " + pki.intermediate.certificate().getBasicConstraints());
    }

    private static void act5Revocation(StringBuilder out, Pki pki) {
        line(out, "FIVE. Revocation, and the bill.");
        X509Certificate root = pki.root.certificate();
        List<X509Certificate> good = List.of(pki.server.certificate(), pki.intermediate.certificate());
        Issued stolen = CertBuilder.issue(CertBuilder.Request.server(Pki.HOST, Pki.START, java.time.LocalDate.of(2026, 6, 1), 77), CertBuilder.newKeys(), pki.intermediate);
        List<X509Certificate> forged = List.of(stolen.certificate(), pki.intermediate.certificate());
        ChainValidator plain = trusting(root);
        line(out, "  someone steals the intermediate's private key and issues their own certificate for shop.example");
        line(out, "  the forged chain is accepted: " + plain.validate(forged, Pki.TODAY).accepted() + "  <- A STOLEN CA KEY BREAKS EVERYTHING BELOW IT");
        ChainValidator revokesServer = new ChainValidator(Set.of(root), Set.of(pki.server.certificate().getSerialNumber()));
        ChainValidator.Result one = revokesServer.validate(good, Pki.TODAY);
        line(out, "  the shop's own certificate put on the revocation list: accepted " + one.accepted() + ", reason " + one.reason());
        ChainValidator revokesMiddle = new ChainValidator(Set.of(root), Set.of(pki.intermediate.certificate().getSerialNumber()));
        line(out, "  the intermediate put on the list: the shop's chain accepted " + revokesMiddle.validate(good, Pki.TODAY).accepted()
                + ", the forged chain accepted " + revokesMiddle.validate(forged, Pki.TODAY).accepted());
        line(out, "  but a list only works if the client gets the latest one: real clients use CRLs, OCSP, short lifetimes, or all three");
        line(out, "  trusting a root is a decision: it may vouch for any name, so keep the store small and keep CA keys offline or in hardware");
    }
}
