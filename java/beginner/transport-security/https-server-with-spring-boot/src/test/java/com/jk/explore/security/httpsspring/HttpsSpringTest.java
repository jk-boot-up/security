package com.jk.explore.security.httpsspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;

/** Each test starts a real Spring Boot server on free loopback ports. */
class HttpsSpringTest {

    @Test
    void trustedClientGetsTlsThirteen() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            Reply reply = new ShopClient(shop.context("trust")).get(shop.https("/products"));
            assertEquals(200, reply.status());
            assertEquals("TLSv1.3", reply.protocol());
            assertEquals("kettle, toaster, lamp", reply.body());
        }
    }

    @Test
    void restClientUsesTheSameTrust() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            assertEquals("kettle, toaster, lamp", new ShopClient(shop.context("trust")).restGet(shop.https("/products")));
        }
    }

    @Test
    void wrongHostNameIsRefused() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            assertThrows(IOException.class, () -> new ShopClient(shop.context("trust")).get("https://evil.example.test:" + shop.httpsPort + "/products"));
        }
    }

    @Test
    void tlsTwelveOnlyClientIsRefused() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            assertThrows(IOException.class, () -> new ShopClient(shop.context("trust"), "TLSv1.2").get(shop.https("/products")));
        }
    }

    @Test
    void defaultJdkTrustRefusesTheDemoRoot() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            assertThrows(IOException.class, () -> new ShopClient(null).get(shop.https("/products")));
        }
    }

    @Test
    void plainPortRedirectsToHttpsWithoutHsts() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            Reply reply = new ShopClient(shop.context("trust")).get(shop.http("/products"));
            assertTrue(reply.status() == 301 || reply.status() == 302 || reply.status() == 307 || reply.status() == 308);
            assertTrue(reply.header("Location").startsWith("https://" + Certificates.HOST + ":" + shop.httpsPort));
            assertEquals("", reply.header("Strict-Transport-Security"));
            assertEquals("", reply.body());
        }
    }

    @Test
    void httpsAnswerCarriesHsts() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            Reply reply = new ShopClient(shop.context("trust")).get(shop.https("/products"));
            assertTrue(reply.header("Strict-Transport-Security").contains("max-age=31536000"));
            assertTrue(reply.header("Strict-Transport-Security").contains("includeSubDomains"));
            assertEquals("nosniff", reply.header("X-Content-Type-Options"));
        }
    }

    @Test
    void sessionCookieIsSecureHttpOnlyAndStrict() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            ShopClient client = new ShopClient(shop.context("trust"));
            String cookie = client.get(shop.https("/login")).header("Set-Cookie");
            assertTrue(cookie.contains("Secure") && cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"), cookie);
            assertTrue(client.wouldSendCookie(shop.https("/products")));
            assertFalse(client.wouldSendCookie(shop.http("/products")));
        }
    }

    @Test
    void renewedCertificateIsServedWithoutRestart() throws Exception {
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", false, true))) {
            String before = Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
            assertEquals(Certificates.fingerprintOf(shop.folder.resolve("shop.p12"), "shop"), before);
            Files.copy(shop.folder.resolve("shop2.p12"), shop.folder.resolve("shop.p12"), StandardCopyOption.REPLACE_EXISTING);
            String after = before;
            for (int i = 0; i < 100 && after.equals(before); i++) {
                Thread.sleep(200);
                after = Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
            }
            assertNotEquals(before, after);
            assertEquals(Certificates.fingerprintOf(shop.folder.resolve("shop2.p12"), "shop"), after);
        }
    }

    @Test
    void withoutReloadTheOldCertificateKeepsBeingServed() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            String before = Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
            Files.copy(shop.folder.resolve("shop2.p12"), shop.folder.resolve("shop.p12"), StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(1500);
            String after = Certificates.fingerprint(new ShopClient(shop.context("trust")).get(shop.https("/products")).serverCertificate());
            assertEquals(before, after);
        }
    }

    @Test
    void mutualTlsAcceptsOnlyCertificatesFromTheRoot() throws Exception {
        try (Shop shop = Shop.start(new Shop.Settings("shop.p12", true, false))) {
            Reply reply = new ShopClient(shop.context("ana")).get(shop.https("/whoami"));
            assertEquals(200, reply.status());
            assertEquals("CN=Ana the customer", reply.body());
            assertThrows(IOException.class, () -> new ShopClient(shop.context("trust")).get(shop.https("/whoami")));
            assertThrows(IOException.class, () -> new ShopClient(shop.context("mallory")).get(shop.https("/whoami")));
        }
    }

    @Test
    void withoutMutualTlsNobodyIsIdentified() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            assertEquals("nobody", new ShopClient(shop.context("trust")).get(shop.https("/whoami")).body());
        }
    }

    @Test
    void errorPagesDoNotLeakAndNoActuatorOrServerHeader() throws Exception {
        try (Shop shop = Shop.start(Shop.Settings.plain())) {
            ShopClient client = new ShopClient(shop.context("trust"));
            Reply boom = client.get(shop.https("/boom"));
            assertEquals(500, boom.status());
            assertFalse(boom.body().contains("hunter2"));
            assertFalse(boom.body().contains("IllegalStateException"));
            assertEquals(404, client.get(shop.https("/actuator/health")).status());
            assertEquals("", client.get(shop.https("/products")).header("Server"));
        }
    }

    @Test
    void closingTheShopRemovesItsFiles() throws Exception {
        Shop shop = Shop.start(Shop.Settings.plain());
        var folder = shop.folder;
        assertTrue(Files.exists(folder));
        shop.close();
        assertFalse(Files.exists(folder));
    }
}
