package com.jk.explore.security.https;

import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Ports are random and never printed. */
public class HttpsDemo {

    static final String SHOP = TestCertificates.HOST;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

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

    /** One point per header, added up in the open: "1 + 1 + 1 + 1 + 1 = 5 of 5". */
    private static String points(HttpResponse<String> response) {
        var headers = new java.util.TreeMap<String, String>();
        response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(headers);
        StringBuilder sum = new StringBuilder();
        for (HeaderAudit.Finding finding : findings) {
            sum.append(sum.length() == 0 ? "" : " + ").append(finding.passed() ? 1 : 0);
        }
        return sum + " = " + HeaderAudit.score(findings) + " of 5";
    }

    private static void act1Secure(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "ONE. An HTTPS request that is checked.");
        HttpResponse<String> response = new ShopClient(trust).get("https://" + SHOP + ":" + shop.httpsPort() + "/cart");
        SSLSession session = response.sslSession().orElseThrow();
        X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
        String names = cert.getSubjectAlternativeNames().stream().map(n -> String.valueOf(n.get(1))).collect(Collectors.joining(", "));
        line(out, "  THE CERTIFICATE (what the shop presents):");
        line(out, "    subject      " + cert.getSubjectX500Principal().getName());
        line(out, "    issuer       " + cert.getIssuerX500Principal().getName()
                + (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal()) ? "  (itself)" : ""));
        line(out, "    names        " + names);
        line(out, "    valid        " + DAY.format(cert.getNotBefore().toInstant()) + " to " + DAY.format(cert.getNotAfter().toInstant()));
        boolean trusted = cert.equals(TestCertificates.shopCertificate());
        boolean named = names.equals(SHOP);
        boolean current;
        try {
            cert.checkValidity();
            current = true;
        } catch (java.security.cert.CertificateException e) {
            current = false;
        }
        line(out, "  THE CLIENT'S THREE CHECKS:");
        line(out, "    trusted      the one certificate the client trusts: " + yes(trusted));
        line(out, "    name         asked for " + SHOP);
        line(out, "                 the certificate names " + names + ": " + yes(named));
        line(out, "    current      today is between the two dates: " + yes(current));
        line(out, "  THE RESPONSE (over " + session.getProtocol() + "):");
        line(out, "    " + response.statusCode() + " " + response.body());
        line(out, "    security headers " + points(response));
    }

    private static void act2Redirect(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "TWO. Plain HTTP only redirects.");
        line(out, "  THE REQUEST (to the plain HTTP port):");
        line(out, "    GET http://" + SHOP + "/cart");
        HttpResponse<String> response = new ShopClient(trust).get("http://" + SHOP + ":" + shop.plainPort() + "/cart");
        String location = response.headers().firstValue("Location").orElse("").replace(":" + shop.httpsPort() + "/", ":<https port>/");
        line(out, "  THE RESPONSE:");
        line(out, "    " + response.statusCode() + " Moved Permanently");
        line(out, "    Location: " + location);
        line(out, "    Strict-Transport-Security: "
                + response.headers().firstValue("Strict-Transport-Security").orElse("none (ignored over HTTP)"));
        HttpResponse<String> secure = new ShopClient(trust).get("https://" + SHOP + ":" + shop.httpsPort() + "/cart");
        String hsts = secure.headers().firstValue("Strict-Transport-Security").orElseThrow();
        long seconds = Long.parseLong(hsts.replaceAll(".*max-age=(\\d+).*", "$1"));
        line(out, "  THE NEXT VISIT (the HTTPS answer carries HSTS):");
        line(out, "    Strict-Transport-Security");
        line(out, "      " + hsts);
        line(out, "    " + seconds + " / 86400 seconds a day = " + seconds / 86_400 + " days of HTTPS only");
        line(out, "  the first request still crossed the network in the clear:");
        line(out, "  HSTS protects only the visits after it");
    }

    private static void act3Cookies(StringBuilder out, ShopServer shop, SSLContext trust) throws Exception {
        line(out, "THREE. A cookie that stays on HTTPS.");
        ShopClient client = new ShopClient(trust);
        HttpResponse<String> login = client.get("https://" + SHOP + ":" + shop.httpsPort() + "/login");
        line(out, "  THE COOKIE (the login over HTTPS sets):");
        line(out, "    " + login.headers().firstValue("Set-Cookie").orElse("").replace("abc123", "..."));
        line(out, "  THE NEXT REQUESTS (does the browser attach it?):");
        line(out, "    over https   " + (client.wouldSendCookie("https://" + SHOP + ":" + shop.httpsPort() + "/whoami") ? "sent" : "not sent")
                + "      shop says: " + client.get("https://" + SHOP + ":" + shop.httpsPort() + "/whoami").body());
        line(out, "    over http    " + (client.wouldSendCookie("http://" + SHOP + ":" + shop.plainPort() + "/whoami") ? "sent" : "not sent")
                + "  because of Secure");
        try (ShopServer weak = new ShopServer(TestCertificates.server("shop.p12"), false, "TLSv1.3", "TLSv1.2")) {
            ShopClient other = new ShopClient(trust);
            HttpResponse<String> weakLogin = other.get("https://" + SHOP + ":" + weak.httpsPort() + "/login");
            line(out, "  BEFORE THE FIX (a shop whose cookie has no Secure):");
            line(out, "    " + weakLogin.headers().firstValue("Set-Cookie").orElse("").replace("abc123", "..."));
            line(out, "    over http    " + (other.wouldSendCookie("http://" + SHOP + ":" + weak.plainPort() + "/whoami") ? "sent" : "not sent")
                    + "      <- A LEAK");
        }
    }

    private static void act4Refusals(StringBuilder out, SSLContext trust) throws Exception {
        line(out, "FOUR. What the client refuses.");
        line(out, "  THE ATTEMPTS:");
        line(out, "    a  " + SHOP + ", a certificate nobody trusts");
        line(out, "    b  the shop's certificate, reached as evil.example.test");
        line(out, "    c  client: TLS 1.3 only     server: TLS 1.2 only");
        line(out, "    d  client: TLS 1.3 or 1.2   server: TLS 1.2 only");
        String a;
        String b;
        String c;
        String d;
        try (ShopServer impostor = new ShopServer(TestCertificates.server("impostor.p12"), true, "TLSv1.3", "TLSv1.2")) {
            a = new ShopClient(trust).attempt("https://" + SHOP + ":" + impostor.httpsPort() + "/cart");
        }
        try (ShopServer shop = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.3", "TLSv1.2")) {
            b = new ShopClient(trust).attempt("https://evil.example.test:" + shop.httpsPort() + "/cart");
        }
        try (ShopServer old = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.2")) {
            c = new ShopClient(trust, "TLSv1.3").attempt("https://" + SHOP + ":" + old.httpsPort() + "/cart");
            d = new ShopClient(trust, "TLSv1.3", "TLSv1.2").attempt("https://" + SHOP + ":" + old.httpsPort() + "/cart");
        }
        line(out, "  THE RESULTS:");
        line(out, "    a  " + a + "  not trusted");
        line(out, "    b  " + b + "  wrong name");
        line(out, "    c  " + c + "  no common version");
        line(out, "    d  " + d);
        line(out, "  allowing the older version is the client's choice, and");
        line(out, "  it is exactly what a downgrade attack asks for");
    }

    private static void act5Checklist(StringBuilder out, SSLContext trust) throws Exception {
        line(out, "FIVE. Scoring a shop from outside.");
        try (ShopServer strong = new ShopServer(TestCertificates.server("shop.p12"), true, "TLSv1.3", "TLSv1.2");
                ShopServer weak = new ShopServer(TestCertificates.server("shop.p12"), false, "TLSv1.2")) {
            List<HttpsChecklist.Check> good = HttpsChecklist.inspect(strong, trust);
            List<HttpsChecklist.Check> bad = HttpsChecklist.inspect(weak, trust);
            line(out, "  THE CHECKS (the same five, on both shops):");
            line(out, "    " + String.format("%-37s", "") + "careful  careless");
            for (int i = 0; i < good.size(); i++) {
                line(out, "    " + String.format("%-37s", good.get(i).name()) + String.format("%-9s", yes(good.get(i))) + yes(bad.get(i)));
            }
            line(out, "  THE SCORES (one point per check):");
            line(out, "    careful    " + sum(good));
            line(out, "    careless   " + sum(bad));
        }
        line(out, "  a padlock proves who the server is and that the connection");
        line(out, "  is private; it says nothing about the rest of the setup");
    }

    private static String yes(HttpsChecklist.Check check) {
        return yes(check.passed());
    }

    private static String yes(boolean flag) {
        return flag ? "yes" : "no";
    }

    private static String sum(List<HttpsChecklist.Check> checks) {
        return checks.stream().map(c -> c.passed() ? "1" : "0").collect(Collectors.joining(" + "))
                + " = " + HttpsChecklist.score(checks) + " of 5";
    }
}
