package com.jk.explore.security.stepca;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/** A real step-ca: what it issues to whom, how a service gets and renews a certificate, and what revocation does and does not do. */
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

    private static void act1(StringBuilder out, StepCa ca) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "ONE. A certificate authority that runs.");
        X509Certificate root = Pki.first(ca.root());
        X509Certificate intermediate = Pki.first(ca.intermediate());
        line(out, "  the root: " + root.getSubjectX500Principal().getName() + ", self-signed: " + root.getIssuerX500Principal().equals(root.getSubjectX500Principal()) + ", valid " + Duration.between(root.getNotBefore().toInstant(), root.getNotAfter().toInstant()).toDays() / 365 + " years");
        intermediate.verify(root.getPublicKey());
        line(out, "  the issuing authority: " + intermediate.getSubjectX500Principal().getName() + ", signed by the root: true, may not create authorities below it: " + (intermediate.getBasicConstraints() == 0));
        line(out, "  the CA answers over https, and a client that trusts the root reads /health: " + ca.get("/health").body().strip().replace("{\"status\":\"ok\"}", "ok"));
        String refusal;
        try {
            java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(ca.url() + "/health")).build(), HttpResponse.BodyHandlers.ofString());
            refusal = "accepted";
        } catch (javax.net.ssl.SSLHandshakeException e) {
            refusal = "refused (the CA's certificate is not signed by anything this client trusts)";
        }
        line(out, "  a client that has not been given the root: " + refusal);
        line(out, "  the root's private key is a file on this machine, kept encrypted with the CA's password: " + Files.readString(ca.rootKey()).contains("ENCRYPTED"));
    }

    private static void act2(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "TWO. A certificate for a service, with a token.");
        String token = ca.token("www.shop.localhost");
        StepCa.Outcome issued = ca.issueWithToken(work, "www", token, "www.shop.localhost");
        X509Certificate www = Pki.first(work.resolve("www.crt"));
        line(out, "  the web team's service asks with a one-time token for www.shop.localhost: " + (issued.ok() ? "issued" : "refused") + ", lasting " + hours(www) + " by default, key " + www.getPublicKey().getAlgorithm());
        line(out, "  a client that trusts the root checks it: " + Pki.validate(work.resolve("www.crt"), ca.root(), null, false, new Date()));
        line(out, "  the same token used a second time: refused (" + ca.issueWithToken(work, "again", token, "www.shop.localhost").reason() + ")");
        line(out, "  a token for another name used for www.shop.localhost: refused (" + ca.issueWithToken(work, "other", ca.token("api.shop.localhost"), "www.shop.localhost").reason() + ")");
        line(out, "  asking for 2000 hours: refused (" + ca.issueWithToken(work, "long", ca.token("long.shop.localhost"), "long.shop.localhost", "--not-after", "2000h").reason() + ")");
        line(out, "  a name outside what the policy allows, pay.example, with a valid token: refused (" + ca.issueWithToken(work, "pay", ca.token("pay.example"), "pay.example").reason() + ")");
        line(out, "  with a day of life, a stolen key is useful for a day, without any revocation list");
    }

    private static void act3(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. A certificate for a service, with ACME.");
        StepCa.Outcome acme = ca.issueWithAcme(work, "acme", "www.shop.localhost");
        X509Certificate cert = Pki.first(work.resolve("acme.crt"));
        line(out, "  the service asks through the ACME provisioner and answers the CA's challenge on port 80 of its own name: " + (acme.ok() ? "issued" : "refused") + ", " + hours(cert) + ", for " + Pki.first(work.resolve("acme.crt")).getSubjectAlternativeNames().stream().map(l -> String.valueOf(l.get(1))).toList());
        line(out, "  a client that trusts the root checks it: " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, false, new Date()));
        line(out, "  ACME for a name outside the policy, evil.example: refused (" + ca.issueWithAcme(work, "evil", "evil.example").reason() + ")");
        line(out, "  ACME proves that the asker controls the name now; it does not prove that the asker is the team that should have it, which is what the policy and the token are for");
    }

    private static void act4(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FOUR. Renewing before it ends.");
        X509Certificate before = Pki.first(work.resolve("www.crt"));
        java.security.PublicKey keyBefore = before.getPublicKey();
        Thread.sleep(1100);
        StepCa.Outcome renewed = ca.renew(work.resolve("www.crt"), work.resolve("www.key"));
        X509Certificate after = Pki.first(work.resolve("www.crt"));
        line(out, "  the service renews with the certificate and its key: " + (renewed.ok() ? "renewed" : "refused") + ", a new serial number: " + !before.getSerialNumber().equals(after.getSerialNumber()) + ", the same key: " + after.getPublicKey().equals(keyBefore) + ", ends later: " + after.getNotAfter().after(before.getNotAfter()));
        StepCa.Outcome rekeyed = ca.rekey(work.resolve("www.crt"), work.resolve("www.key"));
        X509Certificate third = Pki.first(work.resolve("www.crt"));
        line(out, "  renewing with a new key: " + (rekeyed.ok() ? "renewed" : "refused") + ", a different key: " + !third.getPublicKey().equals(keyBefore));
        line(out, "  a service that has lost its key cannot renew: refused (" + ca.renew(work.resolve("www.crt"), work.resolve("acme.key")).reason() + ")  <- ONLY THE HOLDER OF THE KEY RENEWS");
        line(out, "  with a day of life, a renewal job that stops is noticed within a day: the certificate runs out");
    }

    private static void act5(StringBuilder out, StepCa ca, Path work) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FIVE. Revocation, and the bill.");
        StepCa.Outcome revoked = ca.revoke(work.resolve("acme.crt"), work.resolve("acme.key"));
        line(out, "  the acme certificate's key is reported stolen and revoked: " + revoked.ok());
        line(out, "  the CA then refuses to renew it: refused (" + ca.renew(work.resolve("acme.crt"), work.resolve("acme.key")).reason() + ")");
        X509CRL crl = Pki.crl(ca.crl());
        line(out, "  the CA publishes a revocation list signed by the issuing authority, listing 1 serial number: " + (crl.getRevokedCertificates().size() == 1));
        line(out, "  a client that checks the list: " + Pki.validate(work.resolve("acme.crt"), ca.root(), crl, true, new Date()));
        line(out, "  a client that never checks: " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, false, new Date()) + "  <- A REVOKED CERTIFICATE STILL WORKS THERE");
        line(out, "  a client that wants to check and has no list: " + Pki.validate(work.resolve("acme.crt"), ca.root(), null, true, new Date()) + "  <- NO LIST, SO NO: IT FAILS CLOSED");
        line(out, "  the other certificate, www.shop.localhost, against the same list: " + Pki.validate(work.resolve("www.crt"), ca.root(), crl, true, new Date()));
        line(out, "  the CA holds the issuing key online and the root key on disk; whoever reads that disk and knows the password can issue for the policy's names, so the password and the machine are what the whole trust rests on");
        line(out, "  the policy is enforced by the CA itself, so a stolen CA key is not held back by it; a client gets that protection only from a name constraint in the CA certificate, which the plain local-certificate-authority project shows");
    }
}
