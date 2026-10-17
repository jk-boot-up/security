package com.jk.explore.security.httpsspring;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

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

    private static void act1Bundle(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "ONE. TLS from an SSL bundle.");
        line(out, "  the server's certificate and key, and TLS 1.3 only, are set in properties: server.ssl.bundle=shop and a bundle that names the keystore");
        ShopClient client = new ShopClient(shop.context("trust"));
        Reply reply = client.get(shop.https("/products"));
        line(out, "  a client built from a bundle that trusts only the demo root asks for /products: " + reply.status() + " over " + reply.protocol());
        line(out, "  the same call through Spring's RestClient: " + client.restGet(shop.https("/products")));
        line(out, "  the same server reached by another name, evil.example.test: " + client.attempt("https://evil.example.test:" + shop.httpsPort + "/products")
                + "  <- THE NAME ON THE CERTIFICATE MUST MATCH");
        line(out, "  a client that offers only TLS 1.2: " + new ShopClient(shop.context("trust"), "TLSv1.2").attempt(shop.https("/products")));
        line(out, "  a client that uses the JDK's default trust and not the bundle: " + new ShopClient(null).attempt(shop.https("/products"))
                + "  <- THE DEMO ROOT IS NOT IN THE JDK'S LIST");
    }

    private static void act2Redirect(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "TWO. Redirect, HSTS and secure cookies.");
        ShopClient client = new ShopClient(shop.context("trust"));
        Reply plain = client.get(shop.http("/products"));
        line(out, "  a request to the plain HTTP port is answered with " + plain.status() + " pointing to an https address: "
                + plain.header("Location").startsWith("https://" + Certificates.HOST) + ", and the body is empty: " + plain.body().isEmpty());
        line(out, "  the plain answer carries Strict-Transport-Security: " + !plain.header("Strict-Transport-Security").isEmpty());
        Reply secure = client.get(shop.https("/login"));
        line(out, "  the HTTPS answer carries Strict-Transport-Security: " + secure.header("Strict-Transport-Security"));
        String cookie = secure.header("Set-Cookie");
        line(out, "  the session cookie is Secure: " + cookie.contains("Secure") + ", HttpOnly: " + cookie.contains("HttpOnly")
                + ", SameSite=Strict: " + cookie.contains("SameSite=Strict"));
        line(out, "  the client would send that cookie over HTTPS: " + client.wouldSendCookie(shop.https("/products"))
                + ", over plain HTTP: " + client.wouldSendCookie(shop.http("/products")));
    }

    private static void act3Reload(StringBuilder out) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. Renewing a certificate without a restart.");
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", false, true))) {
            String before = fingerprintServed(shop);
            String first = Certificates.fingerprintOf(shop.folder.resolve("shop.p12"), "shop");
            line(out, "  the certificate served matches the file on disk: " + before.equals(first));
            Files.copy(shop.folder.resolve("shop2.p12"), shop.folder.resolve("shop.p12"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String renewed = Certificates.fingerprintOf(shop.folder.resolve("shop.p12"), "shop");
            String after = before;
            for (int i = 0; i < 100 && after.equals(before); i++) {
                Thread.sleep(200);
                after = fingerprintServed(shop);
            }
            line(out, "  the file is replaced with the renewed certificate and the server is not restarted");
            line(out, "  the certificate served has changed: " + !after.equals(before) + ", and now matches the new file: " + after.equals(renewed));
            line(out, "  clients that trust the root still connect without any change: " + new ShopClient(shop.context("trust")).attempt(shop.https("/products")));
        }
    }

    private static String fingerprintServed(Shop shop) throws IOException, InterruptedException, GeneralSecurityException {
        return Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
    }

    private static void act4MutualTls(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Mutual TLS: the server checks the client too.");
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", true, false))) {
            line(out, "  the server is told server.ssl.client-auth=need and the bundle gets a trust store for client certificates");
            Reply reply = new ShopClient(shop.context("ana")).get(shop.https("/whoami"));
            line(out, "  Ana's client presents a certificate signed by the demo root: " + reply.status() + ", the server saw: " + reply.body());
            line(out, "  a client with no certificate: " + new ShopClient(shop.context("trust")).attempt(shop.https("/whoami")));
            line(out, "  a client whose certificate is signed by nobody: " + new ShopClient(shop.context("mallory")).attempt(shop.https("/whoami")));
            line(out, "  this identifies a machine or an app, not a person who types a password, so it suits service-to-service calls");
        }
    }

    private static void act5WhatSpringLeavesToYou(StringBuilder out, Shop shop) throws IOException, InterruptedException {
        line(out, "FIVE. What Spring leaves to you.");
        ShopClient client = new ShopClient(shop.context("trust"));
        Reply actuator = client.get(shop.https("/actuator/health"));
        line(out, "  /actuator/health: " + actuator.status() + "  <- the actuator is not on the class path, so nothing extra is exposed");
        Reply boom = client.get(shop.https("/boom"));
        line(out, "  a failing page answers " + boom.status() + " and its body mentions the internal detail: " + boom.body().contains("hunter2"));
        line(out, "  a Server header that names the software: " + !client.get(shop.https("/products")).header("Server").isEmpty());
        line(out, "  configuration is short, and that is the risk: a wrong property is as silent as a right one, so check the result from outside");
        line(out, "  the private key sits in a file the server can read, so protect it, rotate it, and renew before it expires");
    }
}
