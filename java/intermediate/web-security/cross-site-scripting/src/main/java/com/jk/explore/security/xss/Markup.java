package com.jk.explore.security.xss;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small model of how a browser would read a page: which elements it contains, and what an attribute's value is. It is
 * enough to tell whether a review changed the structure of the page, which is the whole question for cross-site scripting.
 */
final class Markup {

    private static final Pattern TAG = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)([^>]*)>");
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z][a-zA-Z0-9-]*)=\"([^\"]*)\"");

    record Element(String name, String attributes) {
    }

    private Markup() {
    }

    static List<Element> elements(String html) {
        List<Element> found = new ArrayList<>();
        Matcher m = TAG.matcher(html);
        while (m.find()) {
            found.add(new Element(m.group(1).toLowerCase(Locale.ROOT), m.group(2)));
        }
        return found;
    }

    static long count(String html, String name) {
        return elements(html).stream().filter(e -> e.name().equals(name)).count();
    }

    /** The value of the first attribute with this name on the first element with this name, as the browser would read it. */
    static String attribute(String html, String element, String attribute) {
        for (Element e : elements(html)) {
            if (e.name().equals(element)) {
                Matcher m = ATTRIBUTE.matcher(e.attributes());
                while (m.find()) {
                    if (m.group(1).equals(attribute)) {
                        return Encode.decodeHtml(m.group(2));
                    }
                }
            }
        }
        return null;
    }

    /** Script elements and the nonce each one carries (null when it has none). */
    static List<String> scriptNonces(String html) {
        List<String> nonces = new ArrayList<>();
        for (Element e : elements(html)) {
            if (e.name().equals("script")) {
                Matcher m = ATTRIBUTE.matcher(e.attributes());
                String nonce = null;
                while (m.find()) {
                    if (m.group(1).equals("nonce")) {
                        nonce = m.group(2);
                    }
                }
                nonces.add(nonce);
            }
        }
        return nonces;
    }
}
