package com.jk.explore.security.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ParserTest {

    private static final String BASE = "POST /orders HTTP/1.1\r\nHost: shop.example\r\n";

    @Test
    void goodRequestParsesAndRoundTrips() throws Exception {
        String wire = HttpRequest.of("POST", "/orders", "shop.example", "item=1").serialize();
        HttpRequest parsed = HttpParser.parse(wire);
        assertEquals("POST", parsed.method());
        assertEquals("/orders", parsed.target());
        assertEquals("shop.example", parsed.header("HOST"));
        assertArrayEquals("item=1".getBytes(), parsed.body());
        assertEquals(wire, parsed.serialize());
    }

    @Test
    void getWithoutBodyIsFine() throws Exception {
        assertEquals(0, HttpParser.parse("GET / HTTP/1.1\r\nHost: a\r\n\r\n").body().length);
    }

    @Test
    void bothLengthAndTransferEncodingAreRefused() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length: 4\r\nTransfer-Encoding: chunked\r\n\r\nabcd"));
    }

    @Test
    void transferEncodingAloneIsNotSupported() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Transfer-Encoding: chunked\r\n\r\n0\r\n\r\n"));
    }

    @Test
    void conflictingContentLengthsAreRefusedButEqualOnesAreFine() throws Exception {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length: 4\r\nContent-Length: 5\r\n\r\nabcd"));
        assertEquals(4, HttpParser.parse(BASE + "Content-Length: 4\r\nContent-Length: 4\r\n\r\nabcd").body().length);
    }

    @Test
    void spaceBeforeColonIsRefused() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length : 4\r\n\r\nabcd"));
    }

    @Test
    void foldedHeaderIsRefused() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "X-A: b\r\n c\r\n\r\n"));
    }

    @Test
    void bareLineFeedIsRefused() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse("GET / HTTP/1.1\r\nHost: a\nX: b\r\n\r\n"));
    }

    @Test
    void bodyLengthMustMatch() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length: 4\r\n\r\nabcde"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length: 4\r\n\r\nabc"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "Content-Length: +4\r\n\r\nabcd"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse(BASE + "\r\nabc"));
    }

    @Test
    void badRequestLinesAndMissingHostAreRefused() {
        assertThrows(BadMessageException.class, () -> HttpParser.parse("GET / HTTP/1.0\r\nHost: a\r\n\r\n"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse("FETCH / HTTP/1.1\r\nHost: a\r\n\r\n"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse("GET / HTTP/1.1\r\n\r\n"));
        assertThrows(BadMessageException.class, () -> HttpParser.parse("GET / HTTP/1.1\r\nHost: a\r\n"));
    }

    @Test
    void headerInjectionInAResponseIsRefused() throws Exception {
        assertThrows(BadMessageException.class, () -> HttpResponse.redirectToSearch("x\r\nSet-Cookie: a=b").serialize());
        assertThrows(BadMessageException.class, () -> HttpResponse.ok("").with("Bad Name", "v").serialize());
        String wire = HttpResponse.redirectToSearch("shoes").serialize();
        assertEquals("HTTP/1.1 302 Found\r\nLocation: /search?q=shoes\r\nContent-Length: 0\r\n\r\n", wire);
    }
}
