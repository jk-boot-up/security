package com.jk.explore.security.certsbc;

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Keys are random, so only facts that do not depend on them are printed. */
public class CertsBcDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Pki pki = new Pki();
        act1Chain(out, pki);
        act2NameLimits(out, pki);
        act3Lists(out, pki);
        act4Request(out, pki);
        act5Stolen(out, pki);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String name(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName();
    }

    /** The key identifier a certificate names as its issuer's, compared with the issuer's own: the link that lets a client find the right parent. */
    static boolean linkedTo(X509Certificate child, X509Certificate parent) {
        try {
            byte[] authority = AuthorityKeyIdentifier.getInstance(JcaX509ExtensionUtils.parseExtensionValue(child.getExtensionValue(Extension.authorityKeyIdentifier.getId()))).getKeyIdentifierOctets();
            byte[] subject = SubjectKeyIdentifier.getInstance(JcaX509ExtensionUtils.parseExtensionValue(parent.getExtensionValue(Extension.subjectKeyIdentifier.getId()))).getKeyIdentifier();
            return java.util.Arrays.equals(authority, subject);
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static ChainValidator trusting(X509Certificate root) {
        return new ChainValidator(Set.of(root));
    }

    /** The key usage bits 0 to 8 of a certificate as a string of 0 and 1: bit 0 signs data, 5 signs certificates, 6 signs lists. */
    static String usageBits(X509Certificate certificate) {
        StringBuilder bits = new StringBuilder();
        boolean[] usage = certificate.getKeyUsage();
        for (int i = 0; i < 9; i++) {
            bits.append(i < usage.length && usage[i] ? '1' : '0');
        }
        return bits.toString();
    }

    /** The one name an authority's certificate limits it to (its name constraint), or "none". */
    static String permittedName(X509Certificate certificate) {
        byte[] value = certificate.getExtensionValue(Extension.nameConstraints.getId());
        if (value == null) {
            return "none";
        }
        try {
            NameConstraints rule = NameConstraints.getInstance(JcaX509ExtensionUtils.parseExtensionValue(value));
            return String.valueOf(rule.getPermittedSubtrees()[0].getBase().getName());
        } catch (java.io.IOException e) {
            return "unreadable";
        }
    }

    private static void act1Chain(StringBuilder out, Pki pki) {
        line(out, "ONE. A chain built with Bouncy Castle.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        X509Certificate leaf = pki.server.certificate();
        line(out, "  BEFORE BUILDING (what each builder is asked for):");
        line(out, "    root          a CA, 1 more CA below, self-signed");
        line(out, "    intermediate  a CA, 0 below, only names under " + Pki.HOST);
        line(out, "    server        not a CA, for " + Pki.HOST + ", to sign data");
        line(out, "  AFTER BUILDING (read back from the certificates):");
        line(out, "    root          " + name(root) + ", CAs below: " + root.getBasicConstraints());
        line(out, "    intermediate  " + name(middle) + ", CAs below: " + middle.getBasicConstraints());
        line(out, "                  name rule: " + permittedName(middle));
        line(out, "    server        " + name(leaf) + ", a CA: " + (leaf.getBasicConstraints() >= 0));
        line(out, "  KEY USAGE, BIT BY BIT (0 data, 5 certificates, 6 lists):");
        line(out, "    intermediate  " + usageBits(middle) + "  signs certificates and lists");
        line(out, "    server        " + usageBits(leaf) + "  signs data only");
        line(out, "  each names its parent's key identifier: " + linkedTo(leaf, middle) + ", " + linkedTo(middle, root));
    }

    private static void act2NameLimits(StringBuilder out, Pki pki) {
        line(out, "TWO. A limit the authority carries in its own certificate.");
        ChainValidator browser = trusting(pki.root.certificate());
        X509Certificate middle = pki.intermediate.certificate();
        String domain = permittedName(middle);
        List<String> hosts = List.of(Pki.HOST, "api.shop.example", "other.example");
        line(out, "  BEFORE THE CHECK (the intermediate's rule: " + domain + "):");
        line(out, "    " + String.join(", ", hosts));
        line(out, "  STEP. THE RULE BY HAND (the name, or ending in ." + domain + "):");
        for (String host : List.of(Pki.HOST, "api.shop.example", "other.example", "evilshop.example")) {
            line(out, String.format("    %-18s %s: %s", host, NameRule.why(domain, host), NameRule.permits(domain, host)));
        }
        line(out, "  AFTER THE CHECK (the JDK's PKIX, given each chain):");
        long serial = 5;
        for (String host : hosts) {
            X509Certificate leaf = host.equals(Pki.HOST) ? pki.server.certificate() : pki.serverFor(host, serial++).certificate();
            ChainValidator.Result result = browser.validate(List.of(leaf, middle), Pki.TODAY);
            line(out, String.format("    %-18s accepted %s", host, result.accepted()) + (result.accepted() ? "" : ", " + result.reason()));
        }
        line(out, "  a stolen intermediate key can sign only names under " + domain);
    }

    private static String entry(X509CRL list) {
        X509CRLEntry first = list.getRevokedCertificates().iterator().next();
        return "serial " + first.getSerialNumber() + ", " + first.getRevocationReason();
    }

    private static void act3Lists(StringBuilder out, Pki pki) {
        line(out, "THREE. A revocation list signed by the issuer.");
        ChainValidator browser = trusting(pki.root.certificate());
        List<X509Certificate> chain = List.of(pki.server.certificate(), pki.intermediate.certificate());
        X509CRL rootList = Crls.empty(pki.root, Pki.TODAY.minusSeconds(86400));
        X509CRL emptyList = Crls.empty(pki.intermediate, Pki.TODAY.minusSeconds(86400));
        line(out, "  BEFORE REVOCATION (the intermediate's signed list):");
        line(out, "    entries " + Crls.entries(emptyList) + ", the chain accepted: "
                + browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, emptyList)).accepted());
        X509CRL revokedList = Crls.issue(pki.intermediate, Pki.TODAY.minusSeconds(86400), Map.of(pki.server.certificate().getSerialNumber(), Crls.KEY_COMPROMISE));
        ChainValidator.Result revoked = browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, revokedList));
        line(out, "  AFTER REVOCATION (the shop's key is lost):");
        line(out, "    entries " + Crls.entries(revokedList) + ": " + entry(revokedList));
        line(out, "    signed by the intermediate: " + Crls.signedBy(revokedList, pki.intermediate)
                + ", by the root: " + Crls.signedBy(revokedList, pki.root));
        line(out, "    the chain accepted: " + revoked.accepted() + ", " + revoked.reason());
        X509CRL forged = Crls.empty(Pki.rogue()[0], Pki.TODAY.minusSeconds(86400));
        ChainValidator.Result unsigned = browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, forged));
        ChainValidator.Result none = browser.validateWithLists(chain, Pki.TODAY, List.of());
        line(out, "  WITHOUT A GOOD LIST (accepted, and the reason):");
        line(out, "    a stranger's list  " + unsigned.accepted() + ", " + unsigned.reason());
        line(out, "    no list at all     " + none.accepted() + ", " + none.reason());
        line(out, "  this client refuses when it cannot find out; many browsers");
        line(out, "  go ahead instead, which is why short-lived certificates matter");
    }

    private static void act4Request(StringBuilder out, Pki pki) throws Exception {
        line(out, "FOUR. Asking for a certificate without handing over the private key.");
        KeyPair shopKeys = CertFactory.newKeys();
        PKCS10CertificationRequest request = SigningRequest.create(shopKeys, Pki.HOST);
        PKCS10CertificationRequest changed = SigningRequest.parse(SigningRequest.changeName(request, Pki.HOST));
        line(out, "  THE REQUEST (signed by the shop; the private key stays home):");
        line(out, "    names " + SigningRequest.requestedNames(request) + ", public key " + shopKeys.getPublic().getAlgorithm());
        line(out, "    the signature proves the sender holds the key: " + SigningRequest.provesPossession(request));
        line(out, "    one letter changed on the way, it still proves it: " + SigningRequest.provesPossession(changed));
        X509Certificate issued = SigningRequest.certify(request, pki.intermediate, Pki.HOST, 10, LocalDate.of(2025, 6, 1));
        PKCS10CertificationRequest bank = SigningRequest.create(CertFactory.newKeys(), "bank.example");
        String refusal;
        try {
            SigningRequest.certify(bank, pki.intermediate, Pki.HOST, 11, LocalDate.of(2025, 6, 1));
            refusal = "signed";
        } catch (SigningRequest.RefusedException e) {
            refusal = "refused, " + e.getMessage();
        }
        line(out, "  THE RESPONSE (the authority's policy: names under " + Pki.HOST + "):");
        line(out, "    " + Pki.HOST + ": signed, serial " + issued.getSerialNumber() + ", holds the shop's key: "
                + issued.getPublicKey().equals(shopKeys.getPublic()));
        line(out, "    bank.example: " + refusal);
        String pem = Pem.of(issued);
        line(out, "  as PEM: " + pem.lines().findFirst().orElse("") + ", reads back the same: " + Pem.certificate(pem).equals(issued));
    }

    private static void act5Stolen(StringBuilder out, Pki pki) {
        line(out, "FIVE. A stolen authority key, and the bill.");
        ChainValidator browser = trusting(pki.root.certificate());
        X509Certificate middle = pki.intermediate.certificate();
        Issued forged = pki.serverFor(Pki.HOST, 77);
        List<X509Certificate> forgedChain = List.of(forged.certificate(), middle);
        X509CRL rootList = Crls.empty(pki.root, Pki.TODAY.minusSeconds(86400));
        X509CRL middleList = Crls.empty(pki.intermediate, Pki.TODAY.minusSeconds(86400));
        line(out, "  BEFORE REVOCATION (a thief has the intermediate's private key):");
        line(out, "    the thief signs " + Pki.HOST + ", serial " + forged.certificate().getSerialNumber());
        line(out, "    the forged chain accepted: " + browser.validateWithLists(forgedChain, Pki.TODAY, List.of(rootList, middleList)).accepted()
                + "  <- EVERYTHING BELOW FALLS");
        X509CRL rootRevokes = Crls.issue(pki.root, Pki.TODAY.minusSeconds(86400), Map.of(middle.getSerialNumber(), Crls.CA_COMPROMISE));
        List<X509Certificate> realChain = List.of(pki.server.certificate(), middle);
        line(out, "  AFTER REVOCATION (the root's list: " + entry(rootRevokes) + "):");
        line(out, "    the forged chain accepted: " + browser.validateWithLists(forgedChain, Pki.TODAY, List.of(rootRevokes, middleList)).accepted());
        line(out, "    the shop's real chain accepted: " + browser.validateWithLists(realChain, Pki.TODAY, List.of(rootRevokes, middleList)).accepted());
        line(out, "    so the shop needs a new certificate from a new intermediate");
        line(out, "  THE BILL:");
        line(out, "    revocation protects only clients with the new list");
        line(out, "    builders are short to write and easy to get subtly wrong:");
        line(out, "    a missing critical flag or constraint weakens a rule");
        line(out, "    a trusted root may vouch for any name: keep the store small");
        line(out, "    and keep CA keys offline or in hardware");
    }
}
