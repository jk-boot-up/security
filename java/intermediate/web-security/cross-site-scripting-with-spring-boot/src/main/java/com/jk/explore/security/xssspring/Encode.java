package com.jk.explore.security.xssspring;

/** Reads an HTML-escaped value back as a browser would, so a test can check that a value survived the page whole. */
final class Encode {

    private Encode() {
    }

    /** Reverses the escaping Thymeleaf applies to text and attribute values. */
    static String decodeHtml(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }
}
