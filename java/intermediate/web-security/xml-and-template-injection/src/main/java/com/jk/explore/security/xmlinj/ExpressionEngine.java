package com.jk.explore.security.xmlinj;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The first version of the e-mail template engine: {@code ${customer.name}} walks object properties by reflection. That is convenient for
 * the shop owner who writes the template, and fatal when the <em>customer's text</em> becomes part of the template, because the whole application
 * is reachable from {@code customer} and {@code app}.
 */
public final class ExpressionEngine implements TemplateEngine {

    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{([^}]*)}");

    private final App app;

    public ExpressionEngine(App app) {
        this.app = app;
    }

    @Override
    public String render(String template, Customer customer) {
        Map<String, Object> context = Map.of("customer", customer, "app", app);
        Matcher m = EXPRESSION.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(evaluate(m.group(1).trim(), context)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String evaluate(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object value = context.get(parts[0]);
        try {
            for (int i = 1; i < parts.length && value != null; i++) {
                value = property(value, parts[i]);
            }
            return String.valueOf(value);
        } catch (ReflectiveOperationException e) {
            return "[error]";
        }
    }

    private static Object property(Object target, String name) throws ReflectiveOperationException {
        String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && (m.getName().equals(name) || m.getName().equals(getter))) {
                return m.invoke(target);
            }
        }
        throw new NoSuchMethodException(name);
    }
}
