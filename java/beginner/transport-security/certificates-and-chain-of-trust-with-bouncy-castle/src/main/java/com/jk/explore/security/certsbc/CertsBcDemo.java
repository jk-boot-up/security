package com.jk.explore.security.certsbc;

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
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

    private static void act1Chain(StringBuilder out, Pki pki) {
        line(out, "ONE. A chain built with Bouncy Castle.");
        X509Certificate root = pki.root.certificate();
        X509Certificate middle = pki.intermediate.certificate();
        X509Certificate leaf = pki.server.certificate();
        line(out, "  root:         " + name(root) + ", self-signed: " + root.getIssuerX500Principal().equals(root.getSubjectX500Principal())
                + ", authorities allowed below it: " + root.getBasicConstraints());
        line(out, "  intermediate: " + name(middle) + ", authorities allowed below it: " + middle.getBasicConstraints());
        line(out, "  server:       " + name(leaf) + ", may issue certificates: " + (leaf.getBasicConstraints() >= 0));
        boolean[] authorityUse = middle.getKeyUsage();
        boolean[] serverUse = leaf.getKeyUsage();
        line(out, "  an authority's key may sign certificates and lists: " + (authorityUse[5] && authorityUse[6]) + ", and may not sign data: " + !authorityUse[0]);
        line(out, "  the server's key may sign data: " + serverUse[0] + ", and may not sign certificates: " + !serverUse[5]);
        line(out, "  each certificate names its parent's key identifier: server to intermediate " + linkedTo(leaf, middle)
                + ", intermediate to root " + linkedTo(middle, root));
        line(out, "  the server certificate names " + Pki.HOST + ": " + HostName.matches(leaf, Pki.HOST));
        line(out, "  the library wrote the extensions from named builders; no byte of the encoding was written by hand");
    }

    private static void act2NameLimits(StringBuilder out, Pki pki) {
        line(out, "TWO. A limit the authority carries in its own certificate.");
        ChainValidator browser = trusting(pki.root.certificate());
        X509Certificate middle = pki.intermediate.certificate();
        line(out, "  the intermediate may only certify " + Pki.HOST + " and names below it (a name constraint)");
        ChainValidator.Result good = browser.validate(List.of(pki.server.certificate(), middle), Pki.TODAY);
        line(out, "  the certificate for shop.example: accepted " + good.accepted());
        ChainValidator.Result sub = browser.validate(List.of(pki.serverFor("api.shop.example", 5).certificate(), middle), Pki.TODAY);
        line(out, "  a certificate for api.shop.example from the same intermediate: accepted " + sub.accepted());
        ChainValidator.Result other = browser.validate(List.of(pki.serverFor("other.example", 6).certificate(), middle), Pki.TODAY);
        line(out, "  a certificate for other.example from the same intermediate: accepted " + other.accepted() + ", reason " + other.reason());
        line(out, "  the limit is checked by the client, so a stolen intermediate key can only be misused for the names it was limited to");
    }

    private static void act3Lists(StringBuilder out, Pki pki) {
        line(out, "THREE. A revocation list signed by the issuer.");
        ChainValidator browser = trusting(pki.root.certificate());
        List<X509Certificate> chain = List.of(pki.server.certificate(), pki.intermediate.certificate());
        X509CRL rootList = Crls.empty(pki.root, Pki.TODAY.minusSeconds(86400));
        X509CRL emptyList = Crls.empty(pki.intermediate, Pki.TODAY.minusSeconds(86400));
        ChainValidator.Result fine = browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, emptyList));
        line(out, "  the issuer publishes a signed list; it names " + Crls.entries(emptyList) + " revoked certificates and the chain is accepted: " + fine.accepted());
        X509CRL revokedList = Crls.issue(pki.intermediate, Pki.TODAY.minusSeconds(86400), Map.of(pki.server.certificate().getSerialNumber(), Crls.KEY_COMPROMISE));
        ChainValidator.Result revoked = browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, revokedList));
        line(out, "  the shop's key is lost, so the intermediate lists its serial number (" + Crls.entries(revokedList) + " entry): accepted "
                + revoked.accepted() + ", reason " + revoked.reason());
        line(out, "  the list's signature verifies with the intermediate's key: " + Crls.signedBy(revokedList, pki.intermediate)
                + ", with the root's key: " + Crls.signedBy(revokedList, pki.root));
        X509CRL forged = Crls.empty(Pki.rogue()[0], Pki.TODAY.minusSeconds(86400));
        ChainValidator.Result unsigned = browser.validateWithLists(chain, Pki.TODAY, List.of(rootList, forged));
        line(out, "  a list signed by someone else is not used: accepted " + unsigned.accepted() + ", reason " + unsigned.reason());
        ChainValidator.Result none = browser.validateWithLists(chain, Pki.TODAY, List.of());
        line(out, "  no list at all: accepted " + none.accepted() + ", reason " + none.reason());
        line(out, "  this client refuses when it cannot find out; many browsers go ahead instead, which is why short-lived certificates matter");
    }

    private static void act4Request(StringBuilder out, Pki pki) throws Exception {
        line(out, "FOUR. Asking for a certificate without handing over the private key.");
        KeyPair shopKeys = CertFactory.newKeys();
        PKCS10CertificationRequest request = SigningRequest.create(shopKeys, Pki.HOST);
        line(out, "  the shop makes its own key pair and signs a request naming " + SigningRequest.requestedNames(request) + "; the private key stays with the shop");
        line(out, "  the request's signature proves the sender holds the private key: " + SigningRequest.provesPossession(request));
        PKCS10CertificationRequest changed = SigningRequest.parse(SigningRequest.changeName(request, Pki.HOST));
        line(out, "  one letter of the name changed on the way: the signature still proves possession: " + SigningRequest.provesPossession(changed));
        X509Certificate issued = SigningRequest.certify(request, pki.intermediate, Pki.HOST, 10, LocalDate.of(2025, 6, 1));
        line(out, "  the authority's policy allows names under " + Pki.HOST + ", so it signs: the certificate holds the shop's public key: "
                + issued.getPublicKey().equals(shopKeys.getPublic()));
        PKCS10CertificationRequest bank = SigningRequest.create(CertFactory.newKeys(), "bank.example");
        String refusal;
        try {
            SigningRequest.certify(bank, pki.intermediate, Pki.HOST, 11, LocalDate.of(2025, 6, 1));
            refusal = "signed";
        } catch (SigningRequest.RefusedException e) {
            refusal = "refused (" + e.getMessage() + ")";
        }
        line(out, "  a request for bank.example: " + refusal);
        String pem = Pem.of(issued);
        line(out, "  as PEM text it starts with " + pem.lines().findFirst().orElse("") + " and reads back as the same certificate: "
                + Pem.certificate(pem).equals(issued));
    }

    private static void act5Stolen(StringBuilder out, Pki pki) {
        line(out, "FIVE. A stolen authority key, and the bill.");
        ChainValidator browser = trusting(pki.root.certificate());
        X509Certificate middle = pki.intermediate.certificate();
        Issued forged = pki.serverFor(Pki.HOST, 77);
        List<X509Certificate> forgedChain = List.of(forged.certificate(), middle);
        X509CRL rootList = Crls.empty(pki.root, Pki.TODAY.minusSeconds(86400));
        X509CRL middleList = Crls.empty(pki.intermediate, Pki.TODAY.minusSeconds(86400));
        line(out, "  someone steals the intermediate's private key and signs their own certificate for shop.example");
        line(out, "  the forged chain is accepted: " + browser.validateWithLists(forgedChain, Pki.TODAY, List.of(rootList, middleList)).accepted()
                + "  <- A STOLEN CA KEY BREAKS EVERYTHING BELOW IT");
        X509CRL rootRevokes = Crls.issue(pki.root, Pki.TODAY.minusSeconds(86400), Map.of(middle.getSerialNumber(), Crls.CA_COMPROMISE));
        List<X509Certificate> realChain = List.of(pki.server.certificate(), middle);
        line(out, "  the root lists the intermediate as compromised: the forged chain accepted "
                + browser.validateWithLists(forgedChain, Pki.TODAY, List.of(rootRevokes, middleList)).accepted()
                + ", the shop's real chain accepted " + browser.validateWithLists(realChain, Pki.TODAY, List.of(rootRevokes, middleList)).accepted());
        line(out, "  so the shop needs a new certificate from a new intermediate; revocation protects only clients that fetched the new list");
        line(out, "  the builders make certificates short to write and easy to get subtly wrong: a missing critical flag or constraint silently weakens a rule");
        line(out, "  trusting a root is a decision: it may vouch for any name, so keep the store small and keep authority keys offline or in hardware");
    }
}
