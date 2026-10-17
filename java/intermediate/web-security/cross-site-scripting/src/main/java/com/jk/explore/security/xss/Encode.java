package com.jk.explore.security.xss;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Contextual output encoding: the same text needs a different encoding in each place it is written. Each method turns
 * untrusted text into something that can only ever be read as data in its context, never as markup or code.
 */
final class Encode {

    private Encode() {
    }

    /** Text between tags, such as the body of a review. */
    static String htmlBody(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /** A quoted attribute value, such as title="...". The value must be written inside double quotes. */
    static String attribute(String text) {
        return htmlBody(text);
    }

    /** One query parameter value in a URL, such as ?size=... The rest of the URL is written by the shop, never by the user. */
    static String urlParameter(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** A JavaScript string literal inside a nonce-carrying script: everything but letters, digits, space and a few safe marks becomes \\uXXXX. */
    static String javaScriptString(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) && c < 128 || c == ' ' || c == ',' || c == '.') {
                out.append(c);
            } else {
                out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            }
        }
        return out.toString();
    }

    /** Reverses htmlBody and attribute, as a browser reads an attribute value back. */
    static String decodeHtml(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }
}
