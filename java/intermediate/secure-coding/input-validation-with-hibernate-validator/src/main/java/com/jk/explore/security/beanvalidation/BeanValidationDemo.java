package com.jk.explore.security.beanvalidation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.GroupSequence;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        return lines.isEmpty() ? "accepted" : "refused: " + lines.get(0).substring(lines.get(0).indexOf(": ") + 2);
    }

    private static String row(String key, Object value) {
        return String.format("    %-12s %s", key, value);
    }

    private static String visible(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(c -> out.append(c < 0x80 && c >= 0x20 ? Character.toString(c) : String.format(Locale.ROOT, "[U+%04X]", c)));
        return out.toString();
    }

    private static String cart(Checkout checkout) {
        return checkout.lines().stream().map(l -> l.sku() + " x " + l.quantity()).collect(Collectors.joining(", "));
    }

    private static void act1(StringBuilder out, Checks checks) throws Exception {
        line(out, "ONE. Where validation belongs.");
        Orders orders = new Orders(checks.catalogue());
        Checkout negative = new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-1042", -3)));
        line(out, "  THE REQUEST, SENT WITHOUT THE PAGE:");
        line(out, row("cart", cart(negative)));
        line(out, row("fields", Arrays.stream(Checkout.class.getRecordComponents()).map(RecordComponent::getName).collect(Collectors.joining(", "))));
        line(out, row("", "(no price field: the catalogue has the price)"));
        line(out, "  BEFORE THE FIX (a shop that trusts the page):");
        line(out, row("total", checks.catalogue().price("SHO-1042") + " x -3 = " + orders.placeUnchecked(negative) + "  <- A REFUND"));
        Method place = Orders.class.getMethod("place", Checkout.class);
        Set<ConstraintViolation<Orders>> refused = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {negative}, Basic.Ordered.class);
        Set<ConstraintViolation<Orders>> missing = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {null}, Basic.Ordered.class);
        Set<ConstraintViolation<Orders>> fine = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {honest()}, Basic.Ordered.class);
        ConstraintViolation<Orders> v = refused.iterator().next();
        line(out, "  AFTER THE FIX (constraints on the service's parameter):");
        line(out, row("cart -3", v.getPropertyPath()));
        line(out, row("", v.getMessage()));
        line(out, row("no form", Checks.lines(missing).get(0)));
        line(out, row("honest", (fine.isEmpty() ? "accepted, " : "refused, ") + checks.catalogue().price("SHO-1042") + " x 2 = " + orders.place(honest())));
        line(out, "  HOW THE PATH " + v.getPropertyPath() + " IS BUILT:");
        for (Path.Node node : v.getPropertyPath()) {
            String index = node.getIndex() == null ? "" : ", element " + node.getIndex() + " of the list before it";
            line(out, row(node.getName(), node.getKind().name().toLowerCase(Locale.ROOT) + index));
        }
        line(out, row("message", "from " + Rules.of(Line.class, "quantity").split(" ")[0] + " on Line.quantity"));
    }

    private static void act2(StringBuilder out, Checks checks) {
        line(out, "TWO. Say what is allowed, in annotations.");
        line(out, "  THE RULES, WRITTEN ON THE RECORDS:");
        line(out, row("quantity", Rules.of(Line.class, "quantity")));
        line(out, row("sku", Rules.of(Line.class, "sku")));
        line(out, row("email", Rules.of(Checkout.class, "email")));
        line(out, row("country", Rules.of(Checkout.class, "country")));
        line(out, "  QUANTITY (THE INPUT, THEN THE RESULT):");
        for (int quantity : new int[] {7, 0, 100}) {
            line(out, String.format("    %-20s %s", quantity, first(checks.validator().validateValue(Line.class, "quantity", quantity))));
        }
        line(out, "  SKU:");
        for (String sku : new String[] {"SHO-1042", "sho-1042", "SHO-1042;"}) {
            line(out, String.format("    %-20s %s", "\"" + sku + "\"", first(checks.validator().validateValue(Line.class, "sku", sku, jakarta.validation.groups.Default.class))));
        }
        line(out, "  EMAIL:");
        for (String email : new String[] {"Ben@Shop.EXAMPLE", "ben@@shop.example", "ben@localhost"}) {
            String result = first(checks.validator().validateValue(Checkout.class, "email", email));
            line(out, String.format("    %-20s %s", "\"" + email + "\"", result + (email.endsWith("localhost") ? "  <- WIDER THAN THE SHOP NEEDS" : "")));
        }
        line(out, "  COUNTRY:");
        for (String country : new String[] {"DE", "ZZ", "de"}) {
            line(out, String.format("    %-6s %s", "\"" + country + "\"", first(checks.validator().validateValue(Checkout.class, "country", country))));
        }
        Checkout bad = new Checkout("ben@@shop.example", "ZZ", "1", "Ben K", List.of(new Line("sho-1042", 0)));
        line(out, "  THE INPUT, A FORM WITH SEVERAL BAD FIELDS:");
        line(out, row("email", bad.email()));
        line(out, row("country", bad.country()));
        line(out, row("login", bad.login()));
        line(out, row("cart", cart(bad)));
        List<String> all = checks.check(bad);
        line(out, "  THE RESULT, " + all.size() + " PROBLEMS AT ONCE:");
        for (String problem : all) {
            line(out, "    " + problem);
        }
    }

    private static void act3(StringBuilder out, Checks checks) {
        line(out, "THREE. Your own constraints, across fields, and in an order.");
        String[][] samples = {{"ben​k", "a zero-width space"}, {"ben\nk", "a line break"}, {"BenK", "capitals"},
            {"bеn.k", "Cyrillic е in place of e"}, {"josé", "é typed as two characters"}};
        line(out, "  THE INPUT, FIVE LOGINS:");
        for (String[] sample : samples) {
            line(out, String.format("    %-17s %s", visible(sample[0]), sample[1]));
        }
        line(out, "  THE RESULT:          @NoInvisible  all the login's rules");
        int caughtByOne = 0;
        int caughtByAll = 0;
        for (String[] sample : samples) {
            String invisible = first(checks.validator().validate(new Handle(sample[0])));
            String whole = first(checks.validator().validateValue(Checkout.class, "login", sample[0]));
            caughtByOne += invisible.equals("accepted") ? 0 : 1;
            caughtByAll += whole.equals("accepted") ? 0 : 1;
            line(out, String.format("    %-17s %-13s %s", visible(sample[0]), invisible.split(":")[0], whole.split(":")[0]));
        }
        line(out, row("caught", caughtByOne + " of " + samples.length + " by @NoInvisible, " + caughtByAll + " of " + samples.length + " by all the rules"));
        line(out, "  THE INPUT, A RULE ACROSS TWO FIELDS:");
        String[][] places = {{"DE", "1011"}, {"IN", "110001"}};
        for (String[] place : places) {
            line(out, row(place[0] + " " + place[1], place[0] + " codes match " + PostalCodeMatchesCountry.Check.SHAPES.get(place[0])));
        }
        line(out, "  THE RESULT, ON THE POSTAL CODE:");
        for (String[] place : places) {
            List<String> result = checks.check(new Checkout("ben@shop.example", place[0], place[1], "ben.k", List.of(new Line("SHO-1042", 1))));
            line(out, row(place[0] + " " + place[1], result.isEmpty() ? "accepted" : result.get(0)));
        }
        GroupSequence order = Basic.Ordered.class.getAnnotation(GroupSequence.class);
        String[] what = {"sizes, before anything looks inside", "shapes: @Pattern, @Email, @Min ...", "@InStock: a catalogue lookup"};
        line(out, "  HOW THE GROUPS ORDER THE CHECKS (each runs if all before passed):");
        for (int i = 0; i < order.value().length; i++) {
            line(out, row((i + 1) + " " + order.value()[i].getSimpleName(), what[i]));
        }
        line(out, "  THE INPUT, TWO PRODUCT CODES:");
        line(out, row("sho-1042", "the wrong shape"));
        line(out, row("SHO-9999", "the right shape, but not sold"));
        line(out, "  THE RESULT:");
        for (String sku : new String[] {"sho-1042", "SHO-9999"}) {
            checks.catalogue().reset();
            List<String> result = checks.check(new Checkout("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line(sku, 1))));
            int n = checks.catalogue().lookups();
            line(out, row(sku, n + (n == 1 ? " lookup:  " : " lookups: ") + result.get(0).substring(result.get(0).indexOf(": ") + 2)));
        }
    }

    private static void act4(StringBuilder out, Checks checks) {
        line(out, "FOUR. Limits are validation too.");
        List<Line> many = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            many.add(new Line("SHO-1042", 1));
        }
        String longEmail = "a".repeat(10_000) + "@shop.example";
        line(out, "  THE INPUT, TWO OVERSIZED VALUES:");
        line(out, row("cart", many.size() + " lines"));
        line(out, row("e-mail", longEmail.length() + " characters"));
        line(out, "  THE RESULT, STOPPED IN THE Limits GROUP:");
        checks.catalogue().reset();
        List<String> cart = checks.check(new Checkout("ben@shop.example", "DE", "10115", "ben.k", many));
        line(out, row("cart", cart.get(0) + ", " + checks.catalogue().lookups() + " lookups"));
        List<String> email = checks.check(new Checkout(longEmail, "DE", "10115", "ben.k", List.of(new Line("SHO-1042", 1))));
        line(out, row("e-mail", email.get(0)));
        line(out, "  BEFORE THE FIX, ((a+)+)+b ON n a's AND A !:");
        long previous = 0;
        for (int n : new int[] {8, 12, 16}) {
            Redos.STEPS.set(0);
            checks.check(new Coupon("a".repeat(n) + "!"));
            long steps = Redos.STEPS.get();
            line(out, row(n + " a's", steps + " steps" + (previous == 0 ? "" : String.format(Locale.ROOT, "  (x %.1f)", steps / (double) previous))));
            previous = steps;
        }
        line(out, "    4 more a's: about 2^4 = 16 times the work  <- ReDoS");
        line(out, "  AFTER THE FIX, @Size(max = 20) IN THE Limits GROUP:");
        Redos.STEPS.set(0);
        List<String> long5000 = checks.check(new Coupon("a".repeat(5_000) + "!"));
        line(out, row("5000 a's", long5000.get(0) + ", " + Redos.STEPS.get() + " steps"));
        line(out, "  WHY TWO LEVELS: THE JDK ON THE SIMPLER (a+)+$:");
        Pattern simple = Pattern.compile("(a+)+$");
        previous = 0;
        for (int n : new int[] {8, 16, 32}) {
            Redos.STEPS.set(0);
            Redos.Check.find(simple, "a".repeat(n) + "!");
            long steps = Redos.STEPS.get();
            line(out, row(n + " a's", steps + " steps" + (previous == 0 ? "" : String.format(Locale.ROOT, "  (x %.1f)", steps / (double) previous))));
            previous = steps;
        }
        line(out, "    twice the a's, 3 to 4 times the work: not doubling per a");
    }

    private static void act5(StringBuilder out, Checks checks) {
        line(out, "FIVE. The bill: what validation does not do.");
        String markup = "<b>great</b>";
        line(out, "  THE INPUT, A REVIEW TITLE:");
        line(out, row("title", markup + " (" + markup.length() + " characters)"));
        List<String> review = Checks.lines(checks.validator().validate(new Review(markup)));
        line(out, "  THE RESULT, A MESSAGE THAT REPEATS THE VALUE:");
        line(out, "    " + review.get(0));
        line(out, row("repeats it?", review.get(0).contains("<b>") + "  <- MARKUP IN AN ERROR MESSAGE"));
        String[] coupons = {"${1+1}", "${'a'.concat('b')}"};
        line(out, "  THE INPUT, A COUPON PUT INTO A MESSAGE TEMPLATE:");
        for (String coupon : coupons) {
            line(out, "    " + coupon);
        }
        line(out, "  BEFORE THE FIX (templates may call methods, as old versions did):");
        try (Checks risky = new Checks(new Catalogue(), org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel.BEAN_METHODS)) {
            for (String coupon : coupons) {
                line(out, String.format("    %-19s %s", coupon, message(risky.validator().validate(new Review.Unsafe(coupon)))));
            }
            line(out, "    the visitor's text was run as code  <- EXPRESSION INJECTION");
            line(out, "  AFTER THE FIX:");
            line(out, row("default", message(checks.validator().validate(new Review.Unsafe(coupons[0])))));
            line(out, row("parameter", message(risky.validator().validate(new Review.Safe(coupons[0])))));
        }
        line(out, "  SO:");
        line(out, "    valid text still needs encoding on output and query parameters;");
        line(out, "    each rule is a business decision, written down");
    }

    private static String message(Set<? extends ConstraintViolation<?>> violations) {
        return violations.iterator().next().getMessage();
    }
}
