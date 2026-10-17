package com.jk.explore.security.https;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

class HttpsTest {

    private static final String HOST = TestCertificates.HOST;

    private ShopServer shop(boolean strong, String keystore, String... protocols) throws Exception {
        return new ShopServer(TestCertificates.server(keystore), strong, protocols);
    }

    @Test
    void trustedHttpsRequestSucceedsOverTls13() throws Exception {
        try (ShopServer shop = shop(true, "shop.p12", "TLSv1.3", "TLSv1.2")) {
            HttpResponse<String> response = new ShopClient(TestCertificates.client()).get("https://" + HOST + ":" + shop.httpsPort() + "/cart");
            assertEquals(200, response.statusCode());
            assertEquals("TLSv1.3", response.sslSession().orElseThrow().getProtocol());
        }
    }

    @Test
    void strongShopSendsAllFiveSecurityHeaders() throws Exception {
        try (ShopServer shop = shop(true, "shop.p12", "TLSv1.3")) {
            HttpResponse<String> response = new ShopClient(TestCertificates.client()).get("https://" + HOST + ":" + shop.httpsPort() + "/cart");
            var headers = new java.util.TreeMap<String, String>();
            response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));
            assertEquals(5, HeaderAudit.score(HeaderAudit.audit(headers)));
        }
    }

    @Test
    void plainPortRedirectsAndNeverSendsHsts() throws Exception {
        try (ShopServer shop = shop(true, "shop.p12", "TLSv1.3")) {
            HttpResponse<String> response = new ShopClient(TestCertificates.client()).get("http://" + HOST + ":" + shop.plainPort() + "/cart");
            assertEquals(301, response.statusCode());
            assertTrue(response.headers().firstValue("Location").orElseThrow().startsWith("https://" + HOST + ":" + shop.httpsPort()));
            assertTrue(response.headers().firstValue("Strict-Transport-Security").isEmpty());
        }
    }

    @Test
    void weakShopServesPlainHttpDirectly() throws Exception {
        try (ShopServer shop = shop(false, "shop.p12", "TLSv1.2")) {
            assertEquals(200, new ShopClient(TestCertificates.client()).get("http://" + HOST + ":" + shop.plainPort() + "/cart").statusCode());
        }
    }

    @Test
    void secureCookieStaysOffPlainHttpAndInsecureOneLeaks() throws Exception {
        SSLContext trust = TestCertificates.client();
        try (ShopServer strong = shop(true, "shop.p12", "TLSv1.3"); ShopServer weak = shop(false, "shop.p12", "TLSv1.3")) {
            ShopClient a = new ShopClient(trust);
            a.get("https://" + HOST + ":" + strong.httpsPort() + "/login");
            assertTrue(a.wouldSendCookie("https://" + HOST + ":" + strong.httpsPort() + "/"));
            assertFalse(a.wouldSendCookie("http://" + HOST + ":" + strong.plainPort() + "/"));
            ShopClient b = new ShopClient(trust);
            b.get("https://" + HOST + ":" + weak.httpsPort() + "/login");
            assertTrue(b.wouldSendCookie("http://" + HOST + ":" + weak.plainPort() + "/"));
        }
    }

    @Test
    void cookieAttributesAreSetOnlyByTheStrongShop() throws Exception {
        SSLContext trust = TestCertificates.client();
        try (ShopServer strong = shop(true, "shop.p12", "TLSv1.3"); ShopServer weak = shop(false, "shop.p12", "TLSv1.3")) {
            String good = new ShopClient(trust).get("https://" + HOST + ":" + strong.httpsPort() + "/login").headers().firstValue("Set-Cookie").orElseThrow();
            String bad = new ShopClient(trust).get("https://" + HOST + ":" + weak.httpsPort() + "/login").headers().firstValue("Set-Cookie").orElseThrow();
            assertTrue(good.contains("Secure") && good.contains("HttpOnly") && good.contains("SameSite=Lax"));
            assertFalse(bad.contains("Secure") || bad.contains("HttpOnly") || bad.contains("SameSite"));
        }
    }

    @Test
    void impostorCertificateIsRefused() throws Exception {
        try (ShopServer impostor = shop(true, "impostor.p12", "TLSv1.3")) {
            assertEquals("refused (SSLHandshakeException)", new ShopClient(TestCertificates.client()).attempt("https://" + HOST + ":" + impostor.httpsPort() + "/"));
        }
    }

    @Test
    void wrongHostNameIsRefused() throws Exception {
        try (ShopServer shop = shop(true, "shop.p12", "TLSv1.3")) {
            assertEquals("refused (SSLHandshakeException)", new ShopClient(TestCertificates.client()).attempt("https://evil.example.test:" + shop.httpsPort() + "/"));
        }
    }

    @Test
    void downgradeIsRefusedWhenTheClientDemandsTls13() throws Exception {
        try (ShopServer old = shop(true, "shop.p12", "TLSv1.2")) {
            String url = "https://" + HOST + ":" + old.httpsPort() + "/";
            assertEquals("refused (SSLHandshakeException)", new ShopClient(TestCertificates.client(), "TLSv1.3").attempt(url));
            assertEquals("ok (TLSv1.2)", new ShopClient(TestCertificates.client(), "TLSv1.3", "TLSv1.2").attempt(url));
        }
    }

    @Test
    void checklistScoresStrongFiveAndWeakOne() throws Exception {
        SSLContext trust = TestCertificates.client();
        try (ShopServer strong = shop(true, "shop.p12", "TLSv1.3", "TLSv1.2"); ShopServer weak = shop(false, "shop.p12", "TLSv1.2")) {
            assertEquals(5, HttpsChecklist.score(HttpsChecklist.inspect(strong, trust)));
            assertEquals(1, HttpsChecklist.score(HttpsChecklist.inspect(weak, trust)));
        }
    }

    @Test
    void checklistFailsTheCertificateAndTlsChecksOfAnImpostor() throws Exception {
        try (ShopServer impostor = shop(true, "impostor.p12", "TLSv1.3")) {
            var checks = HttpsChecklist.inspect(impostor, TestCertificates.client());
            assertFalse(checks.get(0).passed());
            assertFalse(checks.get(1).passed());
            assertEquals(1, HttpsChecklist.score(checks));
        }
    }
}
