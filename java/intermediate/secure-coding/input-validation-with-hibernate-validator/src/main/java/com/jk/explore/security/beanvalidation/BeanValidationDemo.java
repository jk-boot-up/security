package com.jk.explore.security.beanvalidation;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BeanValidationDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Checks checks = new Checks(new Catalogue())) {
            act1(out, checks);
            act2(out, checks);
            act3(out, checks);
            act4(out, checks);
            act5(out, checks);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static Checkout honest() {
        return new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-1042", 2)));
    }

    private static String first(Set<? extends ConstraintViolation<?>> violations) {
        List<String> lines = Checks.lines(violations);
        return lines.isEmpty() ? "accepted" : "refused (" + lines.get(0).substring(lines.get(0).indexOf(": ") + 2) + ")";
    }

    private static void act1(StringBuilder out, Checks checks) throws Exception {
        line(out, "ONE. Where validation belongs.");
        Orders orders = new Orders(checks.catalogue());
        Checkout negative = new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-1042", -3)));
        line(out, "  a shop that trusts the page: the same request sent without it, a quantity of -3: total " + orders.placeUnchecked(negative) + "  <- A NEGATIVE TOTAL IS A REFUND");
        Method place = Orders.class.getMethod("place", Checkout.class);
        Set<ConstraintViolation<Orders>> refused = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {negative}, Basic.Ordered.class);
        line(out, "  the service with constraints on its parameter: " + Checks.lines(refused));
        Set<ConstraintViolation<Orders>> missing = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {null}, Basic.Ordered.class);
        line(out, "  and with no form at all: " + Checks.lines(missing));
        Set<ConstraintViolation<Orders>> fine = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {honest()}, Basic.Ordered.class);
        line(out, "  an honest order: accepted " + fine.isEmpty() + ", total " + orders.place(honest()) + " from the catalogue");
        line(out, "  the form has no price at all, so there is nothing for a hidden field to set: " + java.util.Arrays.stream(Checkout.class.getRecordComponents()).map(c -> c.getName()).toList());
        line(out, "  the constraints are written once, on the type, and run wherever the type is checked: at the controller, the service or a test");
    }

    private static void act2(StringBuilder out, Checks checks) {
        line(out, "TWO. Say what is allowed, in annotations.");
        for (int quantity : new int[] {7, 0, 100}) {
            line(out, "  quantity " + quantity + ": " + first(checks.validator().validateValue(Line.class, "quantity", quantity)));
        }
        for (String sku : new String[] {"SHO-1042", "sho-1042", "SHO-1042;"}) {
            line(out, "  sku \"" + sku + "\": " + first(checks.validator().validateValue(Line.class, "sku", sku, jakarta.validation.groups.Default.class)));
        }
        for (String email : new String[] {"Ben@Shop.EXAMPLE", "ben@@shop.example", "ben@localhost"}) {
            line(out, "  email \"" + email + "\": " + first(checks.validator().validateValue(Checkout.class, "email", email)));
        }
        line(out, "  the built-in @Email takes ben@localhost, which the shop's own rule refuses  <- A BUILT-IN CHECK IS WIDER THAN YOUR RULE: ADD A @Pattern FOR WHAT YOU REQUIRE");
        for (String country : new String[] {"DE", "ZZ", "de"}) {
            line(out, "  country \"" + country + "\": " + first(checks.validator().validateValue(Checkout.class, "country", country)));
        }
        Checkout bad = new Checkout("ben@@shop.example", "ZZ", "1", "Ben K", List.of(new Line("sho-1042", 0)));
        List<String> all = checks.check(bad);
        line(out, "  a form with several bad fields reports all of them at once: " + all.size() + ", " + all.stream().map(v -> v.substring(0, v.indexOf(':'))).toList());
        line(out, "  the person fixes everything in one go, and an attacker learns nothing more than the messages say");
    }

    private static void act3(StringBuilder out, Checks checks) {
        line(out, "THREE. Your own constraints, across fields, and in an order.");
        for (String[] sample : new String[][] {{"a zero-width space inside", "ben\u200bk"}, {"a line break", "ben\nk"}, {"capitals", "BenK"}, {"Cyrillic \u0435 in place of e", "b\u0435n.k"}, {"o with an accent, typed as two characters", "jose\u0301"}}) {
            line(out, "  a login with " + sample[0] + ": the invisible-character rule says " + first(checks.validator().validate(new Handle(sample[1]))) + "; the whole login rule says " + first(checks.validator().validateValue(Checkout.class, "login", sample[1])));
        }
        line(out, "  no single rule catches everything: the pattern keeps logins to plain lower-case letters, and the custom rule keeps whatever else is allowed in normal form");
        line(out, "  a postal code that does not fit its country (DE, 1011): " + Checks.lines(checks.validator().validate(new Checkout("ben@shop.example", "DE", "1011", "ben.k", List.of(new Line("SHO-1042", 1))), Basic.Ordered.class)));
        line(out, "  the same code with IN, 110001: " + checks.check(new Checkout("ben@shop.example", "IN", "110001", "ben.k", List.of(new Line("SHO-1042", 1)))));
        checks.catalogue().reset();
        List<String> cheapFailure = checks.check(new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("sho-1042", 1))));
        line(out, "  a sku in the wrong shape: " + cheapFailure + ", and the catalogue was asked " + checks.catalogue().lookups() + " times");
        checks.catalogue().reset();
        List<String> unknown = checks.check(new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-9999", 1))));
        line(out, "  a sku in the right shape that nobody sells: " + unknown + ", and the catalogue was asked " + checks.catalogue().lookups() + " time");
        line(out, "  groups put the checks in order: limits, then shape, then the ones that cost a lookup, each only if the one before found nothing wrong");
    }

    private static void act4(StringBuilder out, Checks checks) {
        line(out, "FOUR. Limits are validation too.");
        checks.catalogue().reset();
        List<Line> many = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            many.add(new Line("SHO-1042", 1));
        }
        List<String> cart = checks.check(new Checkout("ben@shop.example", "DE", "10115", "ben.k", many));
        line(out, "  a cart of 10000 lines: " + cart + ", and the catalogue was asked " + checks.catalogue().lookups() + " times");
        String longEmail = "a".repeat(10_000) + "@shop.example";
        List<String> email = checks.check(new Checkout(longEmail, "DE", "10115", "ben.k", List.of(new Line("SHO-1042", 1))));
        line(out, "  an e-mail address of " + longEmail.length() + " characters: " + email);
        for (int n : new int[] {8, 12, 16}) {
            Redos.STEPS.set(0);
            checks.check(new Coupon("a".repeat(n) + "!"));
            line(out, "  the pattern ((a+)+)+b on " + n + " a's and a !: " + Redos.STEPS.get() + " steps");
        }
        line(out, "  four more characters multiply the work by sixteen, so each one doubles it  <- A FEW DOZEN CHARACTERS CAN STOP A SERVER (ReDoS)");
        Redos.STEPS.set(0);
        List<String> long5000 = checks.check(new Coupon("a".repeat(5_000) + "!"));
        line(out, "  the same pattern on 5000 a's and a !, with @Size(max = 20) in the first group: " + long5000 + ", " + Redos.STEPS.get() + " steps");
        line(out, "  the JDK remembers its way through the simpler (a+)+$ and needs only quadratic time; one more level of nesting is exponential again, so a safe-looking pattern is not a safe pattern");
        line(out, "  so: a length limit that runs before any pattern, and no nested repetition in a pattern that reads input");
    }

    private static void act5(StringBuilder out, Checks checks) {
        line(out, "FIVE. The bill: what validation does not do.");
        String markup = "<b>great</b>";
        line(out, "  a review title \"" + markup + "\" is 12 characters: " + Checks.lines(checks.validator().validate(new Review(markup))));
        line(out, "  the message repeats the visitor's text, markup and all: " + Checks.lines(checks.validator().validate(new Review(markup))).get(0).contains("<b>") + "  <- NEVER SEND RAW INPUT BACK IN A MESSAGE: ENCODE IT, OR LEAVE IT OUT");
        line(out, "  a surname O'Brien and a title <b>ok</b> are valid text; encoding on output and parameters in the query are still the code's job");
        line(out, "  a coupon written into a constraint's own message template, \"${1+1}\": Hibernate Validator 9 gives \"" + message(checks.validator().validate(new Review.Unsafe("${1+1}"))) + "\"");
        try (Checks risky = new Checks(new Catalogue(), org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel.BEAN_METHODS)) {
            line(out, "  the same constraint on a validator set to allow method calls in templates, as old versions did: \"" + message(risky.validator().validate(new Review.Unsafe("${1+1}"))) + "\" and \"" + message(risky.validator().validate(new Review.Unsafe("${'a'.concat('b')}"))) + "\"  <- THE VISITOR'S TEXT IS RUN AS CODE");
            line(out, "  passed as a message parameter, the same text stays text even there: \"" + message(risky.validator().validate(new Review.Safe("${1+1}"))) + "\"");
        }
        line(out, "  validation narrows what reaches the code; each rule is also a business decision, and the people who write the messages are not the people who write the rules");
    }

    private static String message(Set<? extends ConstraintViolation<?>> violations) {
        return violations.iterator().next().getMessage();
    }
}
