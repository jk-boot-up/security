package com.jk.explore.security.xmlinj;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The fixed engine: {@code {{name}}} is looked up in a flat map of the few values a template may use, and the value is HTML-escaped. There are no
 * expressions, no reflection, and the output is never scanned again, so a value that looks like a placeholder stays text.
 */
public final class LogicLessEngine implements TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z_]+)\\s*}}");

    @Override
    public String render(String template, Customer customer) {
        Map<String, String> values = Map.of("name", customer.name(), "email", customer.email());
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : escape(value)));
        }
        m.appendTail(out);
        return out.toString();
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
