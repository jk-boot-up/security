package com.jk.explore.security.certs;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.time.LocalDate;
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

    private static String cn(X509Certificate certificate) {
        return name(certificate).replace("CN=", "");
    }

    private static String dates(X509Certificate certificate) {
        return ChainSteps.day(certificate.getNotBefore()) + " to " + ChainSteps.day(certificate.getNotAfter());
    }

    private static String verdict(List<X509Certificate> chain, X509Certificate root, ChainValidator browser) {
        String failure = ChainSteps.firstFailure(ChainSteps.walk(chain, root, Set.of(), Pki.TODAY));
        return String.format("by hand: %-15s JDK: %s", failure, browser.validate(chain, Pki.TODAY).reason());
    }

    private static void act1Chain(StringBuilder out, Pki pki) {
        line(out, "ONE. A chain of three certificates.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        X509Certificate leaf = pki.server.certificate();
        line(out, "  BEFORE SIGNING (what the shop asks for):");
        line(out, "    name        " + name(leaf) + ", for the site " + Pki.HOST);
        line(out, "    public key  " + leaf.getPublicKey().getAlgorithm() + ", "
                + ((ECPublicKey) leaf.getPublicKey()).getParams().getCurve().getField().getFieldSize() + " bits, the shop's own");
        line(out, "    valid       " + dates(leaf));
        line(out, "  AFTER SIGNING (the certificate, serial " + leaf.getSerialNumber() + "):");
        line(out, "    the same three, and issuer  " + leaf.getIssuerX500Principal().getName());
        line(out, "    signature   " + leaf.getSigAlgName() + ", by the issuer's private key");
        line(out, "    anyone checks it with the issuer's public key: " + ChainSteps.signedBy(leaf, middle));
        line(out, "  THE CHAIN, FROM THE ROOT DOWN:");
        line(out, "    root          " + cn(root) + "     self-signed: " + ChainSteps.signedBy(root, root));
        line(out, "    intermediate  " + cn(middle) + "  CA: " + (middle.getBasicConstraints() >= 0) + ", by the root");
        line(out, "    server        " + cn(leaf) + "        CA: " + (leaf.getBasicConstraints() >= 0) + ", by the intermediate");
    }

    private static void act2GoodChain(StringBuilder out, Pki pki) {
        line(out, "TWO. The browser's check, step by step.");
        X509Certificate root = pki.root.certificate();
        List<X509Certificate> chain = List.of(pki.server.certificate(), pki.intermediate.certificate());
        line(out, "  BEFORE THE CHECK (what the browser has):");
        line(out, "    sent by the server  " + cn(chain.get(0)) + ", " + cn(chain.get(1)));
        line(out, "    already trusted     " + cn(root));
        line(out, "    visiting            " + Pki.HOST + ", on " + LocalDate.ofInstant(Pki.TODAY, java.time.ZoneOffset.UTC));
        List<ChainSteps.Step> steps = ChainSteps.walk(chain, root, Set.of(), Pki.TODAY);
        for (int i = 0; i < steps.size(); i++) {
            ChainSteps.Step step = steps.get(i);
            line(out, "  STEP " + (i + 1) + ". " + cn(step.certificate()) + " AGAINST " + cn(step.next()) + ":");
            line(out, "    names it as the issuer: " + step.issuerNamed());
            line(out, "    its signature checks with that public key: " + step.signatureChecks());
            line(out, "    " + LocalDate.ofInstant(Pki.TODAY, java.time.ZoneOffset.UTC) + " is in " + dates(step.certificate()) + ": " + step.inDates());
            if (i > 0) {
                line(out, "    it may issue certificates (a CA): " + step.mayIssue());
            }
        }
        line(out, "  STEP " + (steps.size() + 1) + ". THE ROOT AND THE NAME:");
        line(out, "    " + cn(root) + " is in the trust store: true");
        line(out, "    " + Pki.HOST + " is in the certificate: " + HostName.matches(chain.get(0), Pki.HOST));
        line(out, "  AFTER THE CHECK:");
        line(out, "    by hand: accepted " + ChainSteps.accepted(steps) + ", the JDK: accepted " + trusting(root).validate(chain, Pki.TODAY).accepted());
        line(out, "    *.example matches shop.example " + HostName.matchesPattern("*.example", "shop.example")
                + ", a.b.example " + HostName.matchesPattern("*.example", "a.b.example"));
    }

    private static void act3Refusals(StringBuilder out, Pki pki) {
        line(out, "THREE. Three reasons to refuse.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        ChainValidator browser = trusting(root);
        Issued expired = pki.expiredServer();
        Issued[] rogue = Pki.rogue();
        line(out, "  BEFORE THE CHECK (three chains the browser is shown):");
        line(out, "    1. " + cn(expired.certificate()) + ", valid " + dates(expired.certificate()));
        line(out, "    2. " + cn(rogue[1].certificate()) + ", from a root also named " + cn(rogue[0].certificate()));
        line(out, "    3. the good chain, but the visitor meant evil.example");
        line(out, "  AFTER THE CHECK (the failed step, and the JDK's reason):");
        List<X509Certificate> late = List.of(expired.certificate(), middle);
        line(out, "    1. " + verdict(late, root, browser));
        List<X509Certificate> foreign = List.of(rogue[1].certificate());
        List<ChainSteps.Step> steps = ChainSteps.walk(foreign, root, Set.of(), Pki.TODAY);
        line(out, "    2. " + verdict(foreign, root, browser));
        line(out, "       (the issuer's name matched: " + steps.get(0).issuerNamed() + ", the key did not)");
        List<X509Certificate> good = List.of(pki.server.certificate(), middle);
        line(out, "    3. chain accepted " + browser.validate(good, Pki.TODAY).accepted() + ", the name evil.example matches "
                + HostName.matches(pki.server.certificate(), "evil.example"));
        line(out, "  names prove nothing, only keys do; chain and name are two checks");
    }

    private static void act4Tampering(StringBuilder out, Pki pki) {
        line(out, "FOUR. Changed certificates, and certificates that overstep.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        ChainValidator browser = trusting(root);
        X509Certificate changed = Tamper.changeName(pki.server.certificate());
        List<X509Certificate> chain = List.of(changed, middle);
        line(out, "  BEFORE THE CHANGE (the signed name):  " + name(pki.server.certificate()));
        line(out, "  AFTER THE CHANGE (one letter):        " + name(changed));
        line(out, "    it still parses: true");
        line(out, "    " + verdict(chain, root, browser));
        Issued forged = CertBuilder.issue(CertBuilder.Request.server("other.example", Pki.START, LocalDate.of(2026, 6, 1), 50), CertBuilder.newKeys(), pki.server);
        List<X509Certificate> overstep = List.of(forged.certificate(), pki.server.certificate(), middle);
        line(out, "  BEFORE THE CHECK (the shop's server key signs other.example):");
        line(out, "    the shop's certificate is a CA: " + (pki.server.certificate().getBasicConstraints() >= 0));
        line(out, "  AFTER THE CHECK:");
        line(out, "    " + verdict(overstep, root, browser));
        line(out, "  the intermediate allows " + middle.getBasicConstraints() + " more CAs below it (path length)");
    }

    private static void act5Revocation(StringBuilder out, Pki pki) {
        line(out, "FIVE. Revocation, and the bill.");
        X509Certificate root = pki.root.certificate();
        List<X509Certificate> good = List.of(pki.server.certificate(), pki.intermediate.certificate());
        Issued stolen = CertBuilder.issue(CertBuilder.Request.server(Pki.HOST, Pki.START, LocalDate.of(2026, 6, 1), 77), CertBuilder.newKeys(), pki.intermediate);
        List<X509Certificate> forged = List.of(stolen.certificate(), pki.intermediate.certificate());
        line(out, "  BEFORE REVOCATION (a thief has the intermediate's private key):");
        line(out, "    the thief issues " + cn(stolen.certificate()) + ", serial " + stolen.certificate().getSerialNumber());
        line(out, "    the forged chain accepted: " + trusting(root).validate(forged, Pki.TODAY).accepted() + "  <- EVERYTHING BELOW FALLS");
        BigInteger shopSerial = pki.server.certificate().getSerialNumber();
        BigInteger middleSerial = pki.intermediate.certificate().getSerialNumber();
        ChainValidator.Result one = new ChainValidator(Set.of(root), Set.of(shopSerial)).validate(good, Pki.TODAY);
        ChainValidator revokesMiddle = new ChainValidator(Set.of(root), Set.of(middleSerial));
        line(out, "  AFTER REVOCATION (serials on the client's list):");
        line(out, "    serial " + shopSerial + " (the shop's)");
        line(out, "       the shop's chain accepted " + one.accepted() + ", " + one.reason());
        line(out, "    serial " + middleSerial + " (the intermediate's)");
        line(out, "       the shop's chain accepted " + revokesMiddle.validate(good, Pki.TODAY).accepted()
                + ", the forged " + revokesMiddle.validate(forged, Pki.TODAY).accepted());
        line(out, "  THE BILL:");
        line(out, "    a list works only if the client has the latest one:");
        line(out, "    real clients use CRLs, OCSP, short lifetimes, or all three");
        line(out, "    a trusted root may vouch for any name: keep the store small");
        line(out, "    and keep CA keys offline or in hardware");
    }
}
