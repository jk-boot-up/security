package com.jk.explore.security.xss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EncodeTest {

    @Test
    void htmlBodyEncodesTheFiveSpecialCharacters() {
        assertEquals("&lt;b&gt;&amp;&quot;&#39;", Encode.htmlBody("<b>&\"'"));
        assertEquals("plain text, 42.", Encode.htmlBody("plain text, 42."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<b>Great boots</b>", "Fits 5\" boots", "it's", "a & b < c > d", "", "Zoë ☃"})
    void encodedTextNeverContainsMarkupAndReadsBackExactly(String text) {
        String body = Encode.htmlBody(text);
        assertFalse(body.contains("<") || body.contains(">") || body.contains("\""));
        assertEquals(text, Encode.decodeHtml(body));
    }

    @Test
    void anAttributeValueCannotEndItsQuotesEarly() {
        String html = "<div data-fit=\"" + Encode.attribute("Fits 5\" boots") + "\">";
        assertEquals("Fits 5\" boots", Markup.attribute(html, "div", "data-fit"));
    }

    @Test
    void aUrlParameterCannotAddParametersOrChangeThePath() {
        String encoded = Encode.urlParameter("size 5 & up=1/../x?y#z");
        assertFalse(encoded.contains("&") || encoded.contains("=") || encoded.contains("/") || encoded.contains("?") || encoded.contains("#"));
        assertEquals("size 5 & up=1/../x?y#z", URLDecoder.decode(encoded, StandardCharsets.UTF_8));
    }

    @Test
    void aScriptStringCannotCloseItsQuotesOrItsScript() {
        String encoded = Encode.javaScriptString("\"; </script> '\\");
        assertFalse(encoded.contains("\"") || encoded.contains("'") || encoded.contains("<") || encoded.contains("/") || encoded.contains("\\\\"));
        assertTrue(encoded.contains("\\u0022") && encoded.contains("\\u003c"));
    }

    @Test
    void scriptEncodingIsTheSameInEveryLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("ar"));
            assertEquals("\\u0027", Encode.javaScriptString("'"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
