package com.jk.explore.security.uploadspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** Real Spring Boot servers over a temporary store layout. */
class FilesSpringTest {

    private static final String SAFE_LIMITS_1 = "spring.servlet.multipart.max-file-size=2KB";
    private static final String SAFE_LIMITS_2 = "spring.servlet.multipart.max-request-size=4KB";

    @Test
    void theNaiveDownloadReadsOutsideTheFolder() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "naive")) {
            assertEquals("read: Invoice 1001: Ana, 42.50", shop.download("invoice-1001.txt"));
            assertEquals("read: DB_PASSWORD=demo-not-real", shop.download("../secrets/db.txt"));
        }
    }

    @Test
    void aTextPrefixCheckLetsASiblingFolderThrough() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "prefix")) {
            assertEquals("refused: outside the invoice folder", shop.download("../secrets/db.txt"));
            assertEquals("read: payroll: demo salaries", shop.download("../invoices-private/payroll.txt"));
        }
    }

    @Test
    void theSafeDownloadChecksNameAndPathAndLinks() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "safe")) {
            Files.createSymbolicLink(layout.invoices().resolve("invoice-5555.txt"), layout.secrets().resolve("db.txt"));
            assertEquals("read: Invoice 1001: Ana, 42.50", shop.download("invoice-1001.txt"));
            assertEquals("refused: not an invoice name", shop.download("../secrets/db.txt"));
            assertEquals("refused: not an invoice name", shop.download("%2e%2e%2fsecrets%2fdb.txt"));
            assertEquals("refused: no such file", shop.download("invoice-9999.txt"));
            assertEquals("refused: link leads outside the invoice folder", shop.download("invoice-5555.txt"));
        }
    }

    @Test
    void springsStaticHandlerRefusesDotDotButFollowsALink() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "safe")) {
            Files.createSymbolicLink(layout.invoices().resolve("invoice-5555.txt"), layout.secrets().resolve("db.txt"));
            assertEquals(200, Wire.get(shop.port(), "/static-invoices/invoice-1001.txt").status());
            assertEquals(400, Wire.get(shop.port(), "/static-invoices/..%2fsecrets/db.txt").status());
            assertEquals(404, Wire.get(shop.port(), "/static-invoices/%2e%2e/secrets/db.txt").status());
            assertEquals("DB_PASSWORD=demo-not-real", Wire.get(shop.port(), "/static-invoices/invoice-5555.txt").body());
        }
    }

    @Test
    void theNaiveUploadKeepsTheCustomersNameAndServesScriptsInline() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "naive")) {
            assertEquals("/uploads/page.html", shop.uploaded("page.html", FilesSpringDemo.HTML_PAGE));
            Wire.Reply page = Wire.get(shop.port(), "/uploads/page.html");
            assertTrue(page.contentType().startsWith("text/html"));
            assertEquals("inline", page.disposition());
            shop.uploaded("../../config/app.properties", "admin=mallory".getBytes(StandardCharsets.UTF_8));
            assertEquals("admin=mallory", Files.readString(layout.config().resolve("app.properties")));
        }
    }

    @Test
    void theSafeUploadMakesItsOwnNameAndDecidesTheTypeFromTheBytes() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "safe", SAFE_LIMITS_1, SAFE_LIMITS_2)) {
            Wire.Reply photo = Wire.upload(shop.port(), "../../config/app.properties", FilesSpringDemo.PNG);
            assertEquals("/uploads/photo-0001.png", photo.body());
            assertEquals("admin=ana", Files.readString(layout.config().resolve("app.properties")));
            Wire.Reply served = Wire.get(shop.port(), photo.body());
            assertEquals("image/png", served.contentType());
            assertEquals("attachment", served.disposition());
            assertTrue(served.noSniff());
            assertEquals("refused: not a PNG or JPEG image", shop.uploaded("page.html", FilesSpringDemo.HTML_PAGE));
            assertEquals("refused: not a PNG or JPEG image", shop.uploaded("cat.png", FilesSpringDemo.HTML_PAGE));
            assertFalse(Files.exists(layout.webroot().resolve("uploads").resolve("photo-0001.png")));
        }
    }

    @Test
    void theSafeShopOnlyServesNamesItMadeItself() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "safe", SAFE_LIMITS_1, SAFE_LIMITS_2)) {
            assertEquals(400, Wire.get(shop.port(), "/uploads/..%2f..%2fconfig%2fapp.properties").status());
            assertEquals("refused: not found", Wire.get(shop.port(), "/uploads/app.properties").body());
        }
    }

    @Test
    void sizeIsLimitedByCodeAndByTheMultipartSettings() throws Exception {
        try (Layout layout = Layout.create(); var shop = FilesSpringDemo.start(layout, "safe", SAFE_LIMITS_1, SAFE_LIMITS_2)) {
            byte[] mid = new byte[1500];
            System.arraycopy(FilesSpringDemo.PNG, 0, mid, 0, FilesSpringDemo.PNG.length);
            assertEquals("refused: size not allowed", shop.uploaded("big.png", mid));
            byte[] huge = new byte[5000];
            System.arraycopy(FilesSpringDemo.PNG, 0, huge, 0, FilesSpringDemo.PNG.length);
            assertEquals(413, Wire.upload(shop.port(), "big.png", huge).status());
        }
    }

    @Test
    void theReturnLinkIsOnlyEverAPathOnThisSite() throws Exception {
        try (Layout layout = Layout.create(); var naive = FilesSpringDemo.start(layout, "naive"); var safe = FilesSpringDemo.start(layout, "safe")) {
            for (String target : new String[] {"https://evil.example/login", "//evil.example", "/\\evil.example", "javascript:steal()", ""}) {
                assertEquals(target, Wire.get(naive.port(), "/continue?returnTo=" + Wire.encode(target)).location());
                assertEquals("/account", Wire.get(safe.port(), "/continue?returnTo=" + Wire.encode(target)).location());
            }
            assertEquals("/orders?page=2", Wire.get(safe.port(), "/continue?returnTo=" + Wire.encode("/orders?page=2")).location());
        }
    }

    @Test
    void anAbsoluteAddressIsOnlyAllowedOnAnExactHostMatch() {
        assertTrue(ReturnLink.isOwnHost("https://shop.example/x"));
        assertFalse(ReturnLink.isOwnHost("https://shop.example.evil.example/x"));
        assertFalse(ReturnLink.isOwnHost("https://shop.example@evil.example/x"));
        assertFalse(ReturnLink.isOwnHost("http://shop.example/x"));
    }
}
