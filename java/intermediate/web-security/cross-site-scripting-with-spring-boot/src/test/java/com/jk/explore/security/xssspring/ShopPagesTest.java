package com.jk.explore.security.xssspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The real Spring Boot store on a free loopback port. */
class ShopPagesTest {

    private static XssSpringDemo.Running shop;

    @BeforeAll
    static void start() {
        shop = XssSpringDemo.start();
    }

    @AfterAll
    static void stop() {
        shop.close();
    }

    @Test
    void thTextEscapesTheReview() throws Exception {
        Wire.Page page = Wire.get(shop.port(), "/product");
        assertEquals(200, page.status());
        assertEquals("&lt;b&gt;Great boots&lt;/b&gt;", XssSpringDemo.between(page.body(), "<p class=\"review\">", "</p>"));
        assertEquals(0, Markup.count(page.body(), "b"));
    }

    @Test
    void thUtextLetsTheReviewBecomeMarkup() throws Exception {
        Wire.Page page = Wire.get(shop.port(), "/product-unescaped");
        assertEquals(1, Markup.count(page.body(), "b"));
    }

    @Test
    void attributeLinkAndScriptValuesSurviveWhole() throws Exception {
        Wire.Page page = Wire.get(shop.port(), "/product");
        assertEquals(ReviewController.FIT_NOTE, Markup.attribute(page.body(), "p", "data-fit"));
        assertEquals("/search?q=size%205%20%26%20up", Markup.attribute(page.body(), "a", "href"));
        String literal = XssSpringDemo.scriptLiteral(page.body());
        assertNotNull(literal);
        assertTrue(literal.startsWith("\"") && literal.endsWith("\""));
        String inside = literal.substring(1, literal.length() - 1);
        assertFalse(inside.replace("\\\"", "").contains("\""), "no unescaped quote ends the string early");
    }

    @Test
    void everyPageHasItsOwnNonceAndOnlyItsScriptMayRun() throws Exception {
        Wire.Page first = Wire.get(shop.port(), "/product");
        Wire.Page second = Wire.get(shop.port(), "/product");
        assertNotNull(first.policy());
        assertNotEquals(first.policy(), second.policy());
        String nonce = XssSpringDemo.between(first.policy(), "'nonce-", "'");
        assertEquals(List.of(nonce), Markup.scriptNonces(first.body()));
        assertTrue(ContentSecurityPolicy.allowsInlineScript(first.policy(), nonce));
        assertFalse(ContentSecurityPolicy.allowsInlineScript(first.policy(), null));
        assertFalse(ContentSecurityPolicy.allowsInlineScript(first.policy(), XssSpringDemo.between(second.policy(), "'nonce-", "'")));
    }

    @Test
    void theSessionCookieIsHttpOnlyAndSameSiteStrict() throws Exception {
        Wire.Page page = Wire.get(shop.port(), "/product");
        String cookie = page.cookies().stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
    }
}
