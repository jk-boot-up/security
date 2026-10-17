package com.jk.explore.security.https;

import java.net.http.HttpResponse;
import java.util.List;
import javax.net.ssl.SSLContext;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Ports are random and never printed. */
public class HttpsDemo {

    static final String SHOP = TestCertificates.HOST;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        SSLContext trust = TestCertificates.client();
        try (ShopServer strong = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.3", "TLSv1.2")) {
            act1Secure(out, strong, trust);
            act2Redirect(out, strong, trust);
            act3Cookies(out, strong, trust);
        }
        act4Refusals(out, trust);
        act5Checklist(out, trust);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Secure(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "ONE. An HTTPS request that is checked.");
        HttpResponse<String> response = new ShopClient(trust).get("https://" + SHOP + ":" + shop.httpsPort() + "/cart");
        line(out, "  the shop's certificate is trusted by the client and matches the name " + SHOP);
        line(out, "  the request is answered: " + response.statusCode() + " " + response.body() + ", over " + response.sslSession().orElseThrow().getProtocol());
        var headers = new java.util.TreeMap<String, String>();
        response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));
        line(out, "  security headers present: " + HeaderAudit.score(HeaderAudit.audit(headers)) + " of 5, including Strict-Transport-Security");
        line(out, "  the client checked three things: that the certificate is one it trusts, that it names the shop, and that it is current");
    }

    private static void act2Redirect(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "TWO. Plain HTTP only redirects.");
        HttpResponse<String> response = new ShopClient(trust).get("http://" + SHOP + ":" + shop.plainPort() + "/cart");
        line(out, "  a request to the plain port gets: " + response.statusCode() + " moved permanently");
        line(out, "  it points to an https address: " + response.headers().firstValue("Location").orElse("").startsWith("https://"));
        line(out, "  the plain answer carries no HSTS header: " + response.headers().firstValue("Strict-Transport-Security").isEmpty() + "  (browsers ignore HSTS sent over HTTP)");
        line(out, "  the first request itself crossed the network in the clear  <- THE FIRST VISIT IS STILL EXPOSED; HSTS protects the later ones");
    }

    private static void act3Cookies(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "THREE. A cookie that stays on HTTPS.");
        ShopClient client = new ShopClient(trust);
        HttpResponse<String> login = client.get("https://" + SHOP + ":" + shop.httpsPort() + "/login");
        line(out, "  the login sets: " + login.headers().firstValue("Set-Cookie").orElse("").replace("abc123", "..."));
        line(out, "  asking again over https, the cookie is sent: " + client.get("https://" + SHOP + ":" + shop.httpsPort() + "/whoami").body());
        line(out, "  asking over plain http, the cookie is sent: " + client.wouldSendCookie("http://" + SHOP + ":" + shop.plainPort() + "/whoami")
                + "  (the Secure flag keeps it off plain connections)");
        try (ShopServer weak = new ShopServer(TestCertificates.server("shop.p12"), false, "TLSv1.3", "TLSv1.2")) {
            ShopClient other = new ShopClient(trust);
            other.get("https://" + SHOP + ":" + weak.httpsPort() + "/login");
            line(out, "  a cookie without the Secure flag, asked over plain http, is sent: "
                    + other.wouldSendCookie("http://" + SHOP + ":" + weak.plainPort() + "/whoami") + "  <- A LEAK");
        }
    }

    private static void act4Refusals(StringBuilder out, SSLContext trust) throws Exception {
        line(out, "FOUR. What the client refuses.");
        try (ShopServer impostor = new ShopServer(TestCertificates.server("impostor.p12"), true, "TLSv1.3", "TLSv1.2")) {
            line(out, "  a server with the right name and a certificate nobody trusts: "
                    + new ShopClient(trust).attempt("https://" + SHOP + ":" + impostor.httpsPort() + "/cart"));
        }
        try (ShopServer shop = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.3", "TLSv1.2")) {
            line(out, "  the right certificate, reached under another name: "
                    + new ShopClient(trust).attempt("https://evil.example.test:" + shop.httpsPort() + "/cart"));
        }
        try (ShopServer old = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.2")) {
            line(out, "  a client that demands TLS 1.3, and a server that offers only 1.2: "
                    + new ShopClient(trust, "TLSv1.3").attempt("https://" + SHOP + ":" + old.httpsPort() + "/cart"));
            line(out, "  the same server, a client that also allows 1.2: " + new ShopClient(trust, "TLSv1.3", "TLSv1.2").attempt("https://" + SHOP + ":" + old.httpsPort() + "/cart"));
        }
        line(out, "  allowing the older version is a choice the client makes, and it is what a downgrade attack asks for");
    }

    private static void act5Checklist(StringBuilder out, SSLContext trust) throws Exception {
        line(out, "FIVE. Scoring a shop from outside.");
        try (ShopServer strong = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.3", "TLSv1.2");
                ShopServer weak = new ShopServer(TestCertificates.server("shop.p12"), false, "TLSv1.2")) {
            List<HttpsChecklist.Check> good = HttpsChecklist.inspect(strong, trust);
            List<HttpsChecklist.Check> bad = HttpsChecklist.inspect(weak, trust);
            line(out, "  the careful shop scores " + HttpsChecklist.score(good) + " of 5");
            for (HttpsChecklist.Check check : bad) {
                line(out, "  the careless shop, " + check.name() + ": " + (check.passed() ? "yes" : "no"));
            }
            line(out, "  the careless shop scores " + HttpsChecklist.score(bad) + " of 5: a valid certificate is only the first item");
        }
        line(out, "  a padlock proves who the server is and that the connection is private; it says nothing about the rest of the setup");
    }
}
