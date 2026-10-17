package com.jk.explore.security.https;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Probes a running shop from outside and scores it against five HTTPS checks, one point each. */
final class HttpsChecklist {

    record Check(String name, boolean passed) {
    }

    private HttpsChecklist() {
    }

    static List<Check> inspect(ShopServer shop, javax.net.ssl.SSLContext trust) throws IOException, InterruptedException {
        String https = "https://" + TestCertificates.HOST + ":" + shop.httpsPort();
        String plain = "http://" + TestCertificates.HOST + ":" + shop.plainPort();
        List<Check> checks = new ArrayList<>();

        boolean tls13 = false;
        boolean certificateOk = false;
        HttpResponse<String> secure = null;
        try {
            secure = new ShopClient(trust, "TLSv1.3").get(https + "/cart");
            tls13 = true;
            certificateOk = true;
        } catch (IOException e) {
            try {
                new ShopClient(trust, "TLSv1.2").get(https + "/cart");
                certificateOk = true;
            } catch (IOException ignored) {
                certificateOk = false;
            }
        }
        checks.add(new Check("the certificate is trusted and matches the host name", certificateOk));
        checks.add(new Check("TLS 1.3 is accepted", tls13));

        boolean redirects = false;
        try {
            HttpResponse<String> response = new ShopClient(trust).get(plain + "/cart");
            redirects = response.statusCode() == 301 && response.headers().firstValue("Location").orElse("").startsWith("https://");
        } catch (IOException ignored) {
            redirects = false;
        }
        checks.add(new Check("plain HTTP redirects to HTTPS", redirects));

        checks.add(new Check("HSTS lasts at least a year", secure != null
                && HeaderAudit.audit(flatten(secure)).stream().anyMatch(f -> f.header().equals("Strict-Transport-Security") && f.passed())));

        boolean cookieOk = false;
        if (certificateOk) {
            try {
                HttpResponse<String> login = new ShopClient(trust).get(https + "/login");
                String cookie = login.headers().firstValue("Set-Cookie").orElse("");
                cookieOk = cookie.contains("Secure") && cookie.contains("HttpOnly") && cookie.contains("SameSite=");
            } catch (IOException ignored) {
                cookieOk = false;
            }
        }
        checks.add(new Check("the session cookie is Secure, HttpOnly and SameSite", cookieOk));
        return checks;
    }

    static int score(List<Check> checks) {
        return (int) checks.stream().filter(Check::passed).count();
    }

    private static Map<String, String> flatten(HttpResponse<String> response) {
        Map<String, String> flat = new java.util.TreeMap<>();
        response.headers().map().forEach((k, v) -> flat.put(k, String.join(", ", v)));
        return flat;
    }
}
