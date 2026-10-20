package com.jk.explore.security.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FilesTest {

    private Layout layout;

    @BeforeEach
    void setUp() throws Exception {
        layout = Layout.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        layout.close();
    }

    @Test
    void naiveDownloadsLeaveTheFolder() {
        assertEquals("DB_PASSWORD=demo-not-real", new NaiveDownloads(layout).read("../secrets/db.txt"));
    }

    @Test
    void aTextPrefixCheckPassesASiblingFolder() {
        Downloads d = new PrefixDownloads(layout);
        assertThrows(Refused.class, () -> d.read("../secrets/db.txt"));
        assertEquals("payroll: demo salaries", d.read("../invoices-private/payroll.txt"));
    }

    @Test
    void safeDownloadsReadHonestInvoicesOnly() {
        Downloads d = new SafeDownloads(layout);
        assertEquals("Invoice 1002: Ben, 17.00", d.read("invoice-1002.txt"));
        for (String bad : new String[] {"../secrets/db.txt", "%2e%2e%2fsecrets%2fdb.txt", "..%2fsecrets%2fdb.txt", "invoice-1001.txt/../../secrets/db.txt",
                "/etc/passwd", "invoice-1001.txt%00.png", "../invoices-private/payroll.txt", "", "invoice-1001.TXT"}) {
            assertThrows(Refused.class, () -> d.read(bad), bad);
        }
    }

    @Test
    void safeDownloadsFollowLinksBeforeTrustingThem() throws Exception {
        Files.createSymbolicLink(layout.invoices().resolve("invoice-5555.txt"), layout.secrets().resolve("db.txt"));
        assertEquals("DB_PASSWORD=demo-not-real", new NaiveDownloads(layout).read("invoice-5555.txt"));
        var e = assertThrows(Refused.class, () -> new SafeDownloads(layout).read("invoice-5555.txt"));
        assertEquals("link leads outside the invoice folder", e.getMessage());
    }

    @Test
    void naiveUploadsOverwriteConfigAndServeScriptsInline() throws Exception {
        Uploads up = new NaiveUploads(layout);
        up.save("../../config/app.properties", "admin=mallory".getBytes(StandardCharsets.UTF_8));
        assertEquals("admin=mallory", Files.readString(layout.config().resolve("app.properties")));
        Served s = new WebRoot(layout.webroot()).serve(up.save("page.html", FilesDemo.HTML_PAGE));
        assertEquals("text/html", s.contentType());
        assertFalse(s.attachment());
    }

    @Test
    void safeUploadsDecideFromTheBytesAndNameThemselves() throws Exception {
        SafeUploads up = new SafeUploads(layout);
        String url = up.save("../../config/app.properties", FilesDemo.PNG);
        assertEquals("/photos/photo-0001.png", url);
        assertEquals("admin=ana", Files.readString(layout.config().resolve("app.properties")));
        assertTrue(Files.exists(layout.uploadsPrivate().resolve("photo-0001.png")));
        assertThrows(Refused.class, () -> up.save("cat.png", FilesDemo.HTML_PAGE));
        assertThrows(Refused.class, () -> up.save("big.png", new byte[SafeUploads.MAX_BYTES + 1]));
        assertThrows(Refused.class, () -> up.save("empty.png", new byte[0]));
    }

    @Test
    void safeUploadsAreServedAsDownloadsWithTheRightTypeAndOnlyByTheirOwnIds() {
        SafeUploads up = new SafeUploads(layout);
        up.save("a.png", FilesDemo.PNG);
        Served s = up.fetch("photo-0001.png");
        assertEquals("image/png", s.contentType());
        assertTrue(s.attachment());
        assertTrue(s.noSniff());
        assertThrows(Refused.class, () -> up.fetch("../config/app.properties"));
    }

    @Test
    void snifferReadsMagicBytes() {
        assertEquals("png", Sniffer.of(FilesDemo.PNG).extension());
        assertEquals("jpg", Sniffer.of(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0}).extension());
        assertEquals(null, Sniffer.of("GIF89a....".getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void returnLinkAllowsOnlyPathsOnThisSite() {
        assertEquals("/orders?page=2", ReturnLink.safe("/orders?page=2"));
        for (String bad : new String[] {"https://evil.example", "//evil.example", "/\\evil.example", "javascript:x", "/ok\r\nSet-Cookie: a=b", "evil.example", null, "",
                "/" + "a".repeat(300)}) {
            assertEquals("/account", ReturnLink.safe(bad), String.valueOf(bad));
        }
    }

    @Test
    void ownHostIsAnExactMatch() {
        assertTrue(ReturnLink.isOwnHost("https://shop.example/x"));
        assertFalse(ReturnLink.isOwnHost("http://shop.example/x"));
        assertFalse(ReturnLink.isOwnHost("https://shop.example.evil.example/x"));
        assertFalse(ReturnLink.isOwnHost("https://evilshop.example/x"));
        assertFalse(ReturnLink.isOwnHost("https://shop.example@evil.example/x"));
        assertFalse(ReturnLink.isOwnHost("not a url"));
    }
}
