package com.jk.explore.security.httpsspring;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/** Runs the online store story in five numbered acts against real Spring Boot servers on the loopback interface. */
public class HttpsSpringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Ports are random, so only port-independent facts are printed. */
    public static String run() throws IOException, InterruptedException, GeneralSecurityException {
        StringBuilder out = new StringBuilder();
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            act1Bundle(out, shop);
            act2Redirect(out, shop);
        }
        act3Reload(out);
        act4MutualTls(out);
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            act5WhatSpringLeavesToYou(out, shop);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String subject(Certificate certificate) {
        return ((X509Certificate) certificate).getSubjectX500Principal().getName();
    }

    private static String issuer(Certificate certificate) {
        return ((X509Certificate) certificate).getIssuerX500Principal().getName();
    }

    private static String property(Shop shop, String name) {
        return name + "=" + shop.context.getEnvironment().getProperty(name).replace("file:" + shop.folder + "/", "file:.../");
    }

    private static void act1Bundle(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "ONE. TLS from an SSL bundle.");
        line(out, "  THE CONFIGURATION (properties, no TLS code):");
        line(out, "    " + property(shop, "server.ssl.bundle"));
        line(out, "    " + property(shop, "spring.ssl.bundle.jks.shop.keystore.location"));
        line(out, "    " + property(shop, "spring.ssl.bundle.jks.shop.options.enabled-protocols"));
        ShopClient client = new ShopClient(shop.context("trust"));
        Reply reply = client.get(shop.https("/products"));
        line(out, "  THE CERTIFICATE (what the shop presents):");
        line(out, "    subject   " + subject(reply.serverCertificate()));
        line(out, "    issuer    " + issuer(reply.serverCertificate()) + "   (the root the trust bundle holds)");
        line(out, "  THE ATTEMPTS:");
        line(out, "    a  the trust bundle, TLS 1.3, " + Certificates.HOST);
        line(out, "    b  the same, reached as evil.example.test");
        line(out, "    c  a client that offers only TLS 1.2");
        line(out, "    d  the JDK's default trust instead of the bundle");
        String b = client.attempt("https://evil.example.test:" + shop.httpsPort + "/products");
        String c = new ShopClient(shop.context("trust"), "TLSv1.2").attempt(shop.https("/products"));
        String d = new ShopClient(null).attempt(shop.https("/products"));
        line(out, "  THE RESULTS:");
        line(out, "    a  " + reply.status() + " over " + reply.protocol());
        line(out, "       Spring's RestClient reads: " + client.restGet(shop.https("/products")));
        line(out, "    b  " + b + "  the name on the certificate must match");
        line(out, "    c  " + c + "  the bundle allows TLS 1.3 only");
        line(out, "    d  " + d + "  the demo root is not in the JDK's list");
    }

    private static void act2Redirect(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "TWO. Redirect, HSTS and secure cookies.");
        ShopClient client = new ShopClient(shop.context("trust"));
        line(out, "  THE REQUEST (to the plain HTTP port):");
        line(out, "    GET http://" + Certificates.HOST + "/products");
        Reply plain = client.get(shop.http("/products"));
        line(out, "  THE RESPONSE:");
        line(out, "    " + plain.status() + "  Location: " + plain.header("Location").replace(":" + shop.httpsPort + "/", ":<https port>/"));
        line(out, "         body: " + (plain.body().isEmpty() ? "empty" : plain.body().length() + " characters")
                + ", Strict-Transport-Security: " + (plain.header("Strict-Transport-Security").isEmpty() ? "none" : "sent"));
        Reply secure = client.get(shop.https("/login"));
        String hsts = secure.header("Strict-Transport-Security");
        long seconds = Long.parseLong(hsts.replaceAll(".*max-age=(\\d+).*", "$1"));
        String cookie = secure.header("Set-Cookie");
        int equals = cookie.indexOf('=');
        int end = cookie.indexOf(';');
        line(out, "  THE HTTPS ANSWER (to GET /login):");
        line(out, "    Strict-Transport-Security:");
        line(out, "      " + hsts);
        line(out, "      " + seconds + " / 86400 seconds a day = " + seconds / 86_400 + " days");
        line(out, "    Set-Cookie: " + cookie.substring(0, equals + 1) + "<" + (end - equals - 1) + " random characters>");
        line(out, "      " + cookie.substring(end + 2));
        line(out, "  THE NEXT REQUESTS (does the client attach the cookie?):");
        line(out, "    over https   " + (client.wouldSendCookie(shop.https("/products")) ? "sent" : "not sent"));
        line(out, "    over http    " + (client.wouldSendCookie(shop.http("/products")) ? "sent" : "not sent") + "  because of Secure");
    }

    private static void act3Reload(StringBuilder out) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. Renewing a certificate without a restart.");
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", false, true))) {
            line(out, "  THE CONFIGURATION:");
            line(out, "    " + property(shop, "spring.ssl.bundle.jks.shop.reload-on-update"));
            List<String> seen = new ArrayList<>();
            String before = fingerprintServed(shop);
            String first = Certificates.fingerprintOf(shop.folder.resolve("shop.p12"), "shop");
            line(out, "  BEFORE (SHA-256 fingerprints, shown as letters):");
            line(out, "    shop.p12 on disk  " + name(seen, first) + "    served  " + name(seen, before));
            Files.copy(shop.folder.resolve("shop2.p12"), shop.folder.resolve("shop.p12"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String renewed = Certificates.fingerprintOf(shop.folder.resolve("shop.p12"), "shop");
            String after = before;
            for (int i = 0; i < 100 && after.equals(before); i++) {
                Thread.sleep(200);
                after = fingerprintServed(shop);
            }
            line(out, "  THE CHANGE: shop.p12 is replaced by the renewed file,");
            line(out, "  and the server is not restarted");
            line(out, "  AFTER:");
            line(out, "    shop.p12 on disk  " + name(seen, renewed) + "    served  " + name(seen, after));
            X509Certificate now = Certificates.certificateOf(shop.folder.resolve("shop.p12"), "shop");
            line(out, "    renewed: " + subject(now) + ", by " + issuer(now));
            line(out, "    a client that trusts the root: "
                    + new ShopClient(shop.context("trust")).attempt(shop.https("/products")) + ", no change");
        }
    }

    /** A fingerprint as a letter, so the output does not depend on keys made on this machine. */
    private static String name(List<String> seen, String fingerprint) {
        if (!seen.contains(fingerprint)) {
            seen.add(fingerprint);
        }
        return String.valueOf((char) ('A' + seen.indexOf(fingerprint)));
    }

    private static String fingerprintServed(Shop shop) throws IOException, InterruptedException, GeneralSecurityException {
        return Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
    }

    private static void act4MutualTls(StringBuilder out) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FOUR. Mutual TLS: the server checks the client too.");
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", true, false))) {
            line(out, "  THE CONFIGURATION:");
            line(out, "    " + property(shop, "server.ssl.client-auth"));
            line(out, "    " + property(shop, "spring.ssl.bundle.jks.shop.truststore.location"));
            X509Certificate ana = Certificates.certificateOf(shop.folder.resolve("client.p12"), "ana");
            X509Certificate mallory = Certificates.certificateOf(shop.folder.resolve("impostor.p12"), "shop");
            line(out, "  THE CLIENTS (the certificate each one presents):");
            line(out, "    ana       " + subject(ana) + ", issued by " + issuer(ana));
            line(out, "    nobody    no certificate");
            line(out, "    mallory   " + subject(mallory) + ", issued by "
                    + (mallory.getIssuerX500Principal().equals(mallory.getSubjectX500Principal()) ? "itself" : issuer(mallory)));
            Reply reply = new ShopClient(shop.context("ana")).get(shop.https("/whoami"));
            line(out, "  THE RESULTS:");
            line(out, "    ana       " + reply.status() + ", the server saw: " + reply.body());
            line(out, "    nobody    " + new ShopClient(shop.context("trust")).attempt(shop.https("/whoami")));
            line(out, "    mallory   " + new ShopClient(shop.context("mallory")).attempt(shop.https("/whoami")));
            line(out, "  this identifies a machine or an app, not a person who types");
            line(out, "  a password, so it suits service-to-service calls");
        }
    }

    private static void act5WhatSpringLeavesToYou(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "FIVE. What Spring leaves to you.");
        ShopClient client = new ShopClient(shop.context("trust"));
        line(out, "  THE REQUESTS:");
        line(out, "    GET /actuator/health  a page many apps expose");
        line(out, "    GET /boom             its error holds a password");
        line(out, "    GET /products         does a header name the server?");
        Reply actuator = client.get(shop.https("/actuator/health"));
        Reply boom = client.get(shop.https("/boom"));
        Reply products = client.get(shop.https("/products"));
        line(out, "  THE RESPONSES:");
        line(out, "    /actuator/health      " + actuator.status() + "  no actuator on the class path");
        line(out, "    /boom                 " + boom.status() + "  the password in the body: " + (boom.body().contains("hunter2") ? "yes" : "no"));
        line(out, "    /products             " + products.status() + "  Server header: "
                + (products.header("Server").isEmpty() ? "none" : products.header("Server")));
        line(out, "  THE BILL:");
        line(out, "    a wrong property fails silently: check from outside");
        line(out, "    the private key is a file: protect, rotate, renew it");
    }
}
