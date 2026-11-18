package com.jk.explore.security.xmlinj;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
                value = method(value, parts[i]).invoke(value);
            }
            return String.valueOf(value);
        } catch (ReflectiveOperationException e) {
            return "[error]";
        }
    }

    /** The same walk as {@link #render}, one step per line: the method called and what it returned. For teaching only. */
    public List<String> trace(String path, Customer customer) throws ReflectiveOperationException {
        String[] parts = path.split("\\.");
        Object value = Map.<String, Object>of("customer", customer, "app", app).get(parts[0]);
        List<String> steps = new ArrayList<>();
        steps.add(parts[0] + " -> " + shown(value));
        for (int i = 1; i < parts.length; i++) {
            Method m = method(value, parts[i]);
            value = m.invoke(value);
            steps.add("." + parts[i] + " -> " + m.getName() + "() -> " + shown(value));
        }
        return steps;
    }

    private static String shown(Object value) {
        return value instanceof String s ? s : value.getClass().getSimpleName();
    }

    /** A public method with no parameters called {@code name} or {@code getName}: any such method, on any object. */
    private static Method method(Object target, String name) throws NoSuchMethodException {
        String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Method m : target.getClass().getMethods()) {
            boolean named = m.getName().equals(name) || m.getName().equals(getter);
            if (m.getParameterCount() == 0 && named) {
                return m;
            }
        }
        throw new NoSuchMethodException(name);
    }
}
