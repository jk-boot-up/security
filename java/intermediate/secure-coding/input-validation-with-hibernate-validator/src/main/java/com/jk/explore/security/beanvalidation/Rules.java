package com.jk.explore.security.beanvalidation;

import jakarta.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A teaching class: reads the constraints written on a record component and prints them short, such as
 * {@code @Pattern([A-Z]{3}-\d{4}) @InStock}, so the demo shows the rules from the code itself rather than from a copy.
 */
public final class Rules {

    private static final List<String> HIDDEN = List.of("message", "groups", "payload", "flags");

    private Rules() {
    }

    /** The constraints on one component of a record (Jakarta constraints land on its field), in the order they are written. */
    public static String of(Class<? extends Record> type, String component) {
        Field field;
        try {
            field = type.getDeclaredField(component);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(component, e);
        }
        return Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Constraint.class))
                .map(Rules::shortly).collect(Collectors.joining(" "));
    }

    /** One constraint: its name, then the attributes that differ from their defaults. */
    static String shortly(Annotation annotation) {
        List<String> parts = new ArrayList<>();
        for (Method m : annotation.annotationType().getDeclaredMethods()) {
            if (HIDDEN.contains(m.getName())) {
                continue;
            }
            Object value = invoke(m, annotation);
            if (!Objects.equals(value, m.getDefaultValue())) {
                boolean main = m.getName().equals("value") || m.getName().equals("regexp");
                parts.add((main ? "" : m.getName() + "=") + value);
            }
        }
        parts.sort(null);
        return "@" + annotation.annotationType().getSimpleName() + (parts.isEmpty() ? "" : "(" + String.join(", ", parts) + ")");
    }

    private static Object invoke(Method m, Annotation annotation) {
        try {
            return m.invoke(annotation);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(m.getName().toLowerCase(Locale.ROOT), e);
        }
    }
}
