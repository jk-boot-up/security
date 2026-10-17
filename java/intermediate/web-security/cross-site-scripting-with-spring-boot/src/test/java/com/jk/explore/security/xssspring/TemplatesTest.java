package com.jk.explore.security.xssspring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** A guard: th:utext switches escaping off, so it may appear only in the one template that exists to show the trap. */
class TemplatesTest {

    @Test
    void onlyTheTrapTemplateUsesUnescapedText() {
        assertEquals(List.of("product-unescaped"), Templates.unescaped());
    }

    @Test
    void everyScriptInTheProductTemplateCarriesTheNonce() {
        String template = Templates.text("product");
        int scripts = template.split("<script", -1).length - 1;
        int withNonce = template.split("<script th:attr=\"nonce=\\$\\{cspNonce\\}\"", -1).length - 1;
        assertEquals(scripts, withNonce);
    }

    @Test
    void markupReadsElementsAndAttributesAsABrowserWould() {
        String html = "<p data-fit=\"Fits 5&quot; boots\">x</p><b>y</b><script nonce=\"n1\"></script>";
        assertEquals(1, Markup.count(html, "b"));
        assertEquals("Fits 5\" boots", Markup.attribute(html, "p", "data-fit"));
        assertEquals(List.of("n1"), Markup.scriptNonces(html));
    }
}
