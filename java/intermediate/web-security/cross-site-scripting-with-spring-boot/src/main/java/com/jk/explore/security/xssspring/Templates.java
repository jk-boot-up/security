package com.jk.explore.security.xssspring;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** The store's templates, read from the class path, so a test can list every place that turns escaping off. */
final class Templates {

    static final List<String> NAMES = List.of("product", "product-unescaped");

    private Templates() {
    }

    static String text(String name) {
        try (InputStream in = Templates.class.getResourceAsStream("/templates/" + name + ".html")) {
            if (in == null) {
                throw new IllegalArgumentException("no template " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The templates that write a value with th:utext. */
    static List<String> unescaped() {
        return NAMES.stream().filter(name -> text(name).contains("th:utext")).toList();
    }

    static int usingUnescapedText() {
        return unescaped().size();
    }
}
