package com.jk.explore.security.bffspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The browser stand-in's cookie rules, against a small local server that sets cookies. */
class ShopperTest {

    private HttpServer server;
    private String url;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/set", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "session=s1; Path=/; Secure; HttpOnly; SameSite=Strict");
            exchange.getResponseHeaders().add("Set-Cookie", "XSRF-TOKEN=x1; Path=/");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.createContext("/echo", exchange -> {
            byte[] bytes = String.valueOf(exchange.getRequestHeaders().getFirst("Cookie")).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void anHttpOnlyCookieIsSentButNeverShownToAScript() throws Exception {
        Shopper s = new Shopper();
        s.get(url + "/set");
        assertEquals(Map.of("XSRF-TOKEN", "x1"), s.documentCookie(url));
        assertTrue(s.get(url + "/echo").body().contains("session=s1"));
        assertEquals(java.util.List.of("session", "XSRF-TOKEN"), s.cookieNames(url));
    }

    @Test
    void aSameSiteCookieIsLeftOffARequestThatAnotherSiteStarted() throws Exception {
        Shopper s = new Shopper();
        s.get(url + "/set");
        String body = s.request("GET", url + "/echo", Map.of(), null, true).body();
        assertFalse(body.contains("session=s1"));
        assertTrue(body.contains("XSRF-TOKEN=x1"));
    }

    @Test
    void aSameSiteCookieFromAResponseToAnotherSitesRequestIsNotAccepted() throws Exception {
        Shopper s = new Shopper();
        s.request("GET", url + "/set", Map.of(), null, true);
        assertEquals(java.util.List.of("XSRF-TOKEN"), s.cookieNames(url));
    }

    @Test
    void theSetCookieHeaderIsKeptForLookingAtItsAttributes() throws Exception {
        Shopper s = new Shopper();
        s.get(url + "/set");
        assertEquals("session=s1; Path=/; Secure; HttpOnly; SameSite=Strict", s.setCookieHeader(url, "session"));
    }
}
