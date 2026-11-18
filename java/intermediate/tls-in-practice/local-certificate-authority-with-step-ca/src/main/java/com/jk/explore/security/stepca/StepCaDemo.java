package com.jk.explore.security.stepca;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;

/**
 * A real step-ca: what it issues to whom, how a service gets and renews a certificate, and what revocation does and does not do.
 * step-ca uses the real clock and random serials, keys and ports, so the demo prints only lifetimes, names, reasons and comparisons, and
 * a placeholder where a value is random.
 */
public class StepCaDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws IOException, InterruptedException, GeneralSecurityException {
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory("stepca-certs");
        try (StepCa ca = new StepCa()) {
            act1(out, ca);
            act2(out, ca, work);
            act3(out, ca, work);
            act4(out, ca, work);
            act5(out, ca, work);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String hours(X509Certificate c) {
        return Duration.between(c.getNotBefore().toInstant(), c.getNotAfter().toInstant()).toHours() + " hours";
    }

    private static String years(X509Certificate c) {
        return Duration.between(c.getNotBefore().toInstant(), c.getNotAfter().toInstant()).toDays() / 365 + " years";
    }

    static String cn(X509Certificate c) {
        for (String part : c.getSubjectX500Principal().getName().split(",")) {
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return c.getSubjectX500Principal().getName();
    }

    private static List<String> sans(X509Certificate c) throws GeneralSecurityException {
        return c.getSubjectAlternativeNames().stream().map(l -> String.valueOf(l.get(1))).toList();
    }

    private static boolean signedBy(X509Certificate subject, X509Certificate issuer) {
        try {
            subject.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean encrypted(Path key) throws IOException {
        return Files.readString(key).contains("ENCRYPTED");
    }

    private static void act1(StringBuilder out, StepCa ca) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "ONE. A certificate authority that runs.");
        X509Certificate root = Pki.first(ca.root());
        X509Certificate intermediate = Pki.first(ca.intermediate());
        line(out, "  THE ROOT (made by step ca init):");
        line(out, "    subject      " + cn(root));
        line(out, "    self-signed  " + root.getIssuerX500Principal().equals(root.getSubjectX500Principal()));
        line(out, "    valid        " + years(root));
        line(out, "    key file     " + ca.home().relativize(ca.rootKey()) + ", encrypted: " + encrypted(ca.rootKey()));
        line(out, "  THE ISSUING AUTHORITY:");
        line(out, "    subject      " + cn(intermediate));
        line(out, "    signed by    the root: " + signedBy(intermediate, root));
        line(out, "    path length  " + intermediate.getBasicConstraints() + ": it may not create authorities");
        line(out, "    valid        " + years(intermediate));
        line(out, "  THE CA'S POLICY (in ca.json):");
        line(out, "    allowed names  " + String.join(", ", ca.allowedNames()));
        line(out, "  THE REQUEST: GET /health over https");
        line(out, "  THE RESPONSES:");
        line(out, "    a client that trusts the root: " + ca.get("/health").body().strip().replace("{\"status\":\"ok\"}", "ok"));
        String refusal;
        try {
            HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(ca.url() + "/health")).build(), HttpResponse.BodyHandlers.ofString());
            refusal = "accepted";
        } catch (SSLHandshakeException e) {
            refusal = "refused (" + String.valueOf(e.getMessage()).replaceFirst("^\\(\\w+\\) ", "").split(":")[0] + ")";
        }
        line(out, "    a client without the root: " + refusal);
    }

    private static void act2(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "TWO. A certificate for a service, with a token.");
        String token = ca.token("www.shop.localhost");
        Token t = Token.parse(token);
        line(out, "  THE TOKEN (step ca token www.shop.localhost):");
        line(out, "    parts        " + t.parts() + ": header.payload.signature");
        line(out, "    header       alg " + t.header().path("alg").asString() + ", typ " + t.header().path("typ").asString());
        line(out, "    sub          " + t.text("sub"));
        line(out, "    sans         " + t.sans());
        line(out, "    iss          " + t.text("iss") + " (the provisioner that signed it)");
        line(out, "    aud          " + t.text("aud").replace(ca.url(), "<ca address>"));
        line(out, "    exp - nbf    " + t.lifetimeSeconds() + " seconds");
        line(out, "    sha          the SHA-256 of the root: " + t.pinsRoot(Pki.first(ca.root())));
        line(out, "    jti          " + (t.text("jti").isEmpty() ? "none" : "<random id, " + t.text("jti").length() + " characters>"));
        StepCa.Outcome issued = ca.issueWithToken(work, "www", token, "www.shop.localhost");
        X509Certificate www = Pki.first(work.resolve("www.crt"));
        line(out, "  THE CERTIFICATE (step ca certificate ... --token):");
        line(out, "    " + (issued.ok() ? "issued" : "refused") + ", " + sans(www) + ", " + hours(www) + ", key " + www.getPublicKey().getAlgorithm());
        line(out, "    serial <random>, issued by " + cn(Pki.first(ca.intermediate())));
        line(out, "    a client that trusts the root: " + Pki.validate(work.resolve("www.crt"), ca.root(), null, false, new Date()));
        line(out, "  THE TOKENS THE CA REFUSES:");
        line(out, "    the same token a second time");
        line(out, "      -> refused (" + ca.issueWithToken(work, "again", token, "www.shop.localhost").reason() + ")");
        line(out, "    a token for api.shop.localhost, used for www");
        line(out, "      -> refused (" + ca.issueWithToken(work, "other", ca.token("api.shop.localhost"), "www.shop.localhost").reason() + ")");
        line(out, "    a valid token, asking for 2000 hours");
        line(out, "      -> refused (" + ca.issueWithToken(work, "long", ca.token("long.shop.localhost"), "long.shop.localhost", "--not-after", "2000h").reason() + ")");
        line(out, "    a valid token for pay.example");
        line(out, "      -> refused (" + ca.issueWithToken(work, "pay", ca.token("pay.example"), "pay.example").reason() + ")");
    }

    private static void act3(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. A certificate for a service, with ACME.");
        line(out, "  THE REQUEST (step ca certificate --provisioner acme):");
        line(out, "    www.shop.localhost; the CA calls it back on port 80");
        StepCa.Outcome acme = ca.issueWithAcme(work, "acme", "www.shop.localhost");
        X509Certificate cert = Pki.first(work.resolve("acme.crt"));
        line(out, "  THE CERTIFICATE:");
        line(out, "    " + (acme.ok() ? "issued" : "refused") + ", " + sans(cert) + ", " + hours(cert) + ", key " + cert.getPublicKey().getAlgorithm());
        line(out, "    serial <random>, issued by " + cn(Pki.first(ca.intermediate())));
        line(out, "    a client that trusts the root: " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, false, new Date()));
        line(out, "  ACME FOR A NAME OUTSIDE THE POLICY (evil.example):");
        line(out, "    -> refused (" + ca.issueWithAcme(work, "evil", "evil.example").reason() + ")");
    }

    private static void act4(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FOUR. Renewing before it ends.");
        X509Certificate before = Pki.first(work.resolve("www.crt"));
        PublicKey keyBefore = before.getPublicKey();
        line(out, "  BEFORE THE RENEWAL (www.shop.localhost):");
        line(out, "    serial <random>, " + hours(before) + ", key " + keyBefore.getAlgorithm());
        Thread.sleep(1100);
        StepCa.Outcome renewed = ca.renew(work.resolve("www.crt"), work.resolve("www.key"));
        X509Certificate after = Pki.first(work.resolve("www.crt"));
        line(out, "  AFTER step ca renew (the certificate and its key):");
        line(out, "    " + (renewed.ok() ? "renewed" : "refused") + ", " + hours(after));
        line(out, "    a new serial: " + !before.getSerialNumber().equals(after.getSerialNumber()) + "; the same key: " + after.getPublicKey().equals(keyBefore)
                + "; ends later: " + after.getNotAfter().after(before.getNotAfter()));
        StepCa.Outcome rekeyed = ca.rekey(work.resolve("www.crt"), work.resolve("www.key"));
        X509Certificate third = Pki.first(work.resolve("www.crt"));
        line(out, "  AFTER step ca rekey:");
        line(out, "    " + (rekeyed.ok() ? "renewed" : "refused") + "; a different key: " + !third.getPublicKey().equals(keyBefore));
        line(out, "  A SERVICE THAT LOST ITS KEY (renews with another key):");
        line(out, "    -> refused (" + ca.renew(work.resolve("www.crt"), work.resolve("acme.key")).reason() + ")");
        line(out, "    <- ONLY THE HOLDER OF THE KEY RENEWS");
    }

    private static void act5(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FIVE. Revocation, and the bill.");
        X509Certificate acme = Pki.first(work.resolve("acme.crt"));
        StepCa.Outcome revoked = ca.revoke(work.resolve("acme.crt"), work.resolve("acme.key"));
        line(out, "  THE REVOCATION (step ca revoke, the acme certificate):");
        line(out, "    revoked: " + revoked.ok());
        line(out, "    a renewal afterwards");
        line(out, "      -> refused (" + ca.renew(work.resolve("acme.crt"), work.resolve("acme.key")).reason() + ")");
        X509CRL crl = Pki.crl(ca.crl());
        X509Certificate intermediate = Pki.first(ca.intermediate());
        boolean signedByIssuing;
        try {
            crl.verify(intermediate.getPublicKey());
            signedByIssuing = true;
        } catch (GeneralSecurityException e) {
            signedByIssuing = false;
        }
        X509CRLEntry entry = crl.getRevokedCertificates().iterator().next();
        line(out, "  THE REVOCATION LIST (GET /1.0/crl):");
        line(out, "    signed by    " + cn(intermediate) + ": " + signedByIssuing);
        line(out, "    entries      " + crl.getRevokedCertificates().size() + "; the acme serial: " + entry.getSerialNumber().equals(acme.getSerialNumber()));
        line(out, "    current for  " + Duration.between(crl.getThisUpdate().toInstant(), crl.getNextUpdate().toInstant()).toMinutes()
                + " minutes (next update - this update)");
        line(out, "  THE CHECKS:");
        line(out, "    acme, the client checks the list");
        line(out, "      -> " + Pki.validate(work.resolve("acme.crt"), ca.root(), crl, true, new Date()));
        line(out, "    acme, a client that never checks");
        line(out, "      -> " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, false, new Date()) + "  <- A REVOKED CERTIFICATE STILL WORKS");
        line(out, "    acme, a client that wants to check but has no list");
        line(out, "      -> " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, true, new Date()));
        line(out, "      <- NO LIST, SO NO: IT FAILS CLOSED");
        line(out, "    www.shop.localhost, the same list");
        line(out, "      -> " + Pki.validate(work.resolve("www.crt"), ca.root(), crl, true, new Date()));
        line(out, "  THE KEYS THE CA HOLDS (the bill):");
        line(out, "    issuing  " + ca.home().relativize(ca.intermediateKey()) + ", online, encrypted: " + encrypted(ca.intermediateKey()));
        line(out, "    root     " + ca.home().relativize(ca.rootKey()) + ", same disk, encrypted: " + encrypted(ca.rootKey()));
    }
}
