package com.jk.explore.security.httpnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests start real nginx containers, so they need a running Docker: run them with ./gradlew integrationTest. */
@Tag("docker")
class NginxContainerTest {

    private static Probe.Reply get(NginxConfig.Variant variant, String path) throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(variant))) {
            return Probe.get(nginx.port(), path);
        }
    }

    private static int status(NginxConfig.Variant variant, String raw) throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(variant))) {
            return Probe.send(nginx.port(), raw).status();
        }
    }

    private static String post(String headers, String body) {
        return "POST /order HTTP/1.1\r\nHost: shop.example\r\n" + headers + "\r\nConnection: close\r\n\r\n" + body;
    }

    @Test
    void asItComesNginxAddsNoHeadersAndShowsItsVersion() throws Exception {
        Probe.Reply reply = get(NginxConfig.Variant.BARE, "/");
        assertEquals(0, HttpNginxDemo.score(reply));
        assertTrue(reply.headers().get("server").contains("1.31.7"));
    }

    @Test
    void withoutAlwaysErrorPagesLoseTheHeaders() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITHOUT_ALWAYS))) {
            assertEquals(5, HttpNginxDemo.score(Probe.get(nginx.port(), "/")));
            Probe.Reply missing = Probe.get(nginx.port(), "/missing");
            assertEquals(404, missing.status());
            assertEquals(0, HttpNginxDemo.score(missing));
        }
    }

    @Test
    void withAlwaysErrorPagesKeepTheHeadersAndTheVersionIsHidden() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.HEADERS_WITH_ALWAYS))) {
            Probe.Reply missing = Probe.get(nginx.port(), "/missing");
            assertEquals(5, HttpNginxDemo.score(missing));
            assertEquals("nginx", Probe.get(nginx.port(), "/").headers().get("server"));
        }
    }

    @Test
    void aNestedAddHeaderDropsTheServerLevelOnes() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.INHERITANCE_TRAP))) {
            assertEquals(5, HttpNginxDemo.score(Probe.get(nginx.port(), "/")));
            Probe.Reply account = Probe.get(nginx.port(), "/account");
            assertEquals(0, HttpNginxDemo.score(account));
            assertEquals("no-store", account.headers().get("cache-control"));
        }
    }

    @Test
    void repeatingTheHeadersInTheNestedLocationFixesIt() throws Exception {
        assertEquals(5, HttpNginxDemo.score(get(NginxConfig.Variant.INHERITANCE_FIXED, "/account")));
    }

    @Test
    void aBodyOverTheLimitIsRefusedAndASmallOneIsNot() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.LIMITS))) {
            assertEquals(200, Probe.send(nginx.port(), post("Content-Length: 100", "x".repeat(100))).status());
            assertEquals(413, Probe.send(nginx.port(), post("Content-Length: 2000", "x".repeat(2000))).status());
        }
    }

    @Test
    void aHugeHeaderLineIsRefused() throws Exception {
        assertEquals(400, status(NginxConfig.Variant.LIMITS,
                "GET / HTTP/1.1\r\nHost: shop.example\r\nX-Pad: " + "a".repeat(4000) + "\r\nConnection: close\r\n\r\n"));
    }

    @Test
    void contentLengthWithTransferEncodingIsRefused() throws Exception {
        assertEquals(400, status(NginxConfig.Variant.LIMITS, post("Content-Length: 4\r\nTransfer-Encoding: chunked", "0\r\n\r\n")));
    }

    @Test
    void twoDifferentContentLengthsAreRefused() throws Exception {
        assertEquals(400, status(NginxConfig.Variant.LIMITS, post("Content-Length: 4\r\nContent-Length: 5", "abcde")));
    }

    @Test
    void aProxyPassesTheApplicationsHeadersUnlessToldNotTo() throws Exception {
        Probe.Reply leaky = get(NginxConfig.Variant.UPSTREAM_LEAKY, "/");
        assertEquals("PHP/5.6.40", leaky.headers().get("x-powered-by"));
        assertTrue(leaky.headers().get("server").contains("1.31.7"));
        Probe.Reply hidden = get(NginxConfig.Variant.UPSTREAM_HIDDEN, "/");
        assertFalse(hidden.headers().containsKey("x-powered-by"));
        assertFalse(hidden.headers().containsKey("x-aspnet-version"));
        assertEquals("nginx", hidden.headers().get("server"));
    }

    @Test
    void closingTheContainerStopsItAndLeavesNoneBehind() throws Exception {
        Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Variant.BARE));
        int port = nginx.port();
        nginx.close();
        assertThrows(IOException.class, () -> Probe.get(port, "/"));
        assertEquals("", runningContainers());
    }

    private static String runningContainers() throws Exception {
        Process p = new ProcessBuilder("docker", "ps", "-q", "--filter", "name=http-nginx-").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes()).strip();
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        return out;
    }

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), HttpNginxDemo.run());
        assertEquals("", runningContainers());
    }
}
