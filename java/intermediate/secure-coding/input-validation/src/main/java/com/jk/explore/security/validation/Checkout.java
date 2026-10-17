package com.jk.explore.security.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The checkout at the trust boundary. Only the expected fields are accepted, each one is parsed into a typed value that cannot hold
 * anything else, every problem is reported at once, and the price comes from the catalogue.
 */
public final class Checkout {

    public static final Set<String> FIELDS = Set.of("sku", "quantity", "email", "country");

    public record Order(Sku sku, Quantity quantity, Email email, CountryCode country, BigDecimal total) {
    }

    public record Result(Order order, List<Rejected> problems) {

        public boolean accepted() {
            return order != null;
        }
    }

    private Checkout() {
    }

    public static Result place(Map<String, String> fields) {
        List<Rejected> problems = new ArrayList<>();
        for (String name : new TreeSet<>(fields.keySet())) {
            if (!FIELDS.contains(name)) {
                problems.add(new Rejected(name.matches("[a-z]{1,20}") ? name : "a field", "not a field of this form"));
            }
        }
        Sku sku = attempt(problems, () -> Sku.parse(fields.get("sku")));
        Quantity quantity = attempt(problems, () -> Quantity.parse(fields.get("quantity")));
        Email email = attempt(problems, () -> Email.parse(fields.get("email")));
        CountryCode country = attempt(problems, () -> CountryCode.parse(fields.get("country")));
        BigDecimal price = null;
        if (sku != null) {
            price = Catalogue.price(sku).orElse(null);
            if (price == null) {
                problems.add(new Rejected("sku", "not a product the store sells"));
            }
        }
        if (!problems.isEmpty()) {
            return new Result(null, List.copyOf(problems));
        }
        return new Result(new Order(sku, quantity, email, country, price.multiply(BigDecimal.valueOf(quantity.value()))), List.of());
    }

    private interface Parse<T> {
        T get() throws Rejected;
    }

    private static <T> T attempt(List<Rejected> problems, Parse<T> parse) {
        try {
            return parse.get();
        } catch (Rejected e) {
            problems.add(e);
            return null;
        }
    }
}
