package com.jk.explore.security.validation;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class ValidationDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        act1Boundary(out);
        act2AllowLists(out);
        act3Canonical(out);
        act4Limits(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static Map<String, String> honest() {
        return Map.of("sku", "SHO-1042", "quantity", "2", "email", "ana@shop.example", "country", "IN");
    }

    private static String problems(Checkout.Result result) {
        List<String> names = new ArrayList<>();
        for (Rejected r : result.problems()) {
            names.add(r.getMessage());
        }
        return String.join("; ", names);
    }

    private static String row(String key, Object value) {
        return String.format("    %-12s %s", key, value);
    }

    private static String fields(Map<String, String> fields) {
        StringBuilder text = new StringBuilder();
        for (String name : List.of("sku", "quantity", "price")) {
            if (fields.containsKey(name)) {
                text.append(text.isEmpty() ? "" : " ").append(name).append('=').append(fields.get(name));
            }
        }
        return text.toString();
    }

    private static void act1Boundary(StringBuilder out) {
        line(out, "ONE. Where validation belongs.");
        Map<String, String> negative = Map.of("sku", "SHO-1042", "quantity", "-3", "email", "ana@shop.example", "country", "IN");
        line(out, "  THE REQUEST, SENT WITHOUT THE CHECKOUT PAGE:");
        line(out, row("fields", fields(negative)));
        line(out, row("the page", "would refuse it (quantity min 1): " + !BrowserForm.wouldSubmit(negative)));
        line(out, "  BEFORE THE FIX (a shop that trusts the page):");
        line(out, row("price", TrustingShop.price(negative) + " from the catalogue"));
        line(out, row("total", TrustingShop.price(negative) + " x " + negative.get("quantity") + " = " + TrustingShop.total(negative)
                + "  <- A REFUND"));
        Map<String, String> cheap = Map.of("sku", "SHO-1042", "quantity", "2", "price", "0.01", "email", "ana@shop.example", "country", "IN");
        Checkout.Result ok = Checkout.place(honest());
        line(out, "  THE REQUEST, WITH THE HIDDEN PRICE FIELD CHANGED:");
        line(out, row("fields", fields(cheap)));
        line(out, "  BEFORE THE FIX:");
        line(out, row("price", TrustingShop.price(cheap) + " from the form"));
        line(out, row("total", TrustingShop.price(cheap) + " x " + cheap.get("quantity") + " = " + TrustingShop.total(cheap)
                + " instead of " + ok.order().total()));
        line(out, "  AFTER THE FIX (the checkout validates on the server):");
        line(out, row("quantity -3", "refused (" + problems(Checkout.place(negative)) + ")"));
        line(out, row("price 0.01", "refused (" + problems(Checkout.place(cheap)) + ")"));
        BigDecimal price = Catalogue.price(ok.order().sku()).orElseThrow();
        line(out, row("honest", "accepted, " + price + " x " + ok.order().quantity().value() + " = " + ok.order().total()
                + " from the catalogue"));
        for (String raw : List.of("-3", "2")) {
            line(out, "  HOW THE SERVER READS quantity \"" + raw + "\":");
            for (Steps.Step step : Steps.quantity(raw)) {
                line(out, row(step.name(), step.result()));
            }
        }
    }

    private static void act2AllowLists(StringBuilder out) {
        line(out, "TWO. Say what is allowed, and parse into types.");
        String[][] groups = {
            {"QUANTITY (digits only, from 1 to 99):", "quantity", "7", "0", "1e2", " 5"},
            {"SKU, THE PRODUCT CODE (such as SHO-1042):", "sku", "SHO-1042", "sho-1042", "SHO-1042;"},
            {"EMAIL (a store's narrow rule, at most 254 characters):", "email", "Ben@Shop.EXAMPLE", "ben@@shop.example", "ben@localhost"},
            {"COUNTRY (the ISO codes the JDK ships):", "country", "DE", "ZZ", "de"},
            {"AMOUNT (exact, at most two decimals):", "amount", "19.50", "19.505", "-5", "NaN"},
        };
        for (String[] group : groups) {
            line(out, "  " + group[0]);
            for (int i = 2; i < group.length; i++) {
                line(out, String.format("    %-19s %s", "\"" + group[i] + "\"", verdict(group[1], group[i])));
            }
        }
        Map<String, String> bad = new TreeMap<>(Map.of("sku", "shoes", "quantity", "lots", "email", "x", "country", "Mars"));
        line(out, "  THE INPUT, A FORM WITH FOUR BAD FIELDS:");
        line(out, "    " + String.join(" ", bad.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList()));
        Checkout.Result all = Checkout.place(bad);
        line(out, "  THE RESULT, " + all.problems().size() + " PROBLEMS REPORTED AT ONCE:");
        for (Rejected r : all.problems()) {
            line(out, "    " + r.getMessage());
        }
    }

    private static String verdict(String field, String raw) {
        try {
            Object value = switch (field) {
                case "quantity" -> Quantity.parse(raw);
                case "sku" -> Sku.parse(raw);
                case "email" -> Email.parse(raw);
                case "country" -> CountryCode.parse(raw);
                default -> Money.parse(raw);
            };
            return "-> " + value;
        } catch (Rejected e) {
            return "refused: " + e.rule();
        }
    }

    private static void act3Canonical(StringBuilder out) {
        line(out, "THREE. Canonicalise before validating.");
        Map<String, String> spellings = new LinkedHashMap<>();
        spellings.put("plain", "ana");
        spellings.put("full-width", "\uFF41\uFF4E\uFF41");
        spellings.put("capitals", "ANA");
        line(out, "  THE INPUT, THREE SPELLINGS OF ONE NAME:");
        spellings.forEach((label, name) -> line(out, row(label, Steps.codePoints(name))));
        Accounts accounts = new Accounts();
        line(out, "  BEFORE THE FIX (a registry that compares raw text):");
        spellings.forEach((label, name) -> line(out, row(label, "new account: " + accounts.registerNaively(name)
                + (label.equals("plain") ? "" : "  <- ONE MORE ACCOUNT"))));
        line(out, "  HOW THE CANONICAL FORM IS MADE (full-width):");
        for (Steps.Step step : Steps.username(spellings.get("full-width"))) {
            line(out, row(step.name(), step.result()));
        }
        line(out, "  AFTER THE FIX (a registry that compares canonical names):");
        spellings.forEach((label, name) -> {
            try {
                accounts.register(name);
                line(out, row(label, "new account: " + Username.parse(name).value()));
            } catch (Rejected e) {
                line(out, row(label, "refused: " + e.rule()));
            }
        });
        String[][] hidden = {
            {"an\u200Ba", "a zero-width space inside"},
            {"ana\nadmin", "a line break inside"},
            {"\u0430na", "a Cyrillic letter in place of a"},
        };
        line(out, "  THE INPUT, HIDDEN AND LOOK-ALIKE LETTERS:");
        for (String[] h : hidden) {
            line(out, String.format("    %-18s %s", Steps.visible(h[0]), h[1]));
        }
        line(out, "  THE RESULT:");
        for (String[] h : hidden) {
            line(out, "    " + nameVerdict(h[0]));
        }
        String precomposed = "Jos\u00E9";
        String decomposed = "Jose\u0301";
        line(out, "  ONE NAME, TWO ENCODINGS (" + Text.nfc(decomposed) + "):");
        line(out, row("one letter", Steps.codePoints(precomposed)));
        line(out, row("e + accent", Steps.codePoints(decomposed)));
        line(out, row("equal?", "as raw text " + precomposed.equals(decomposed)
                + ", after NFC " + Text.nfc(precomposed).equals(Text.nfc(decomposed))));
        line(out, "  DISPLAY NAMES (any script, but no look-alike of the staff):");
        for (String name : List.of("Sh\u043Ep Supp\u043Ert", "Jos\u00E9 Mart\u00EDn")) {
            line(out, row("the input", name + (name.contains("\u043E") ? "  (o is Cyrillic U+043E)" : "")));
            line(out, row("skeleton", DisplayName.skeleton(name)));
            line(out, row("the result", displayVerdict(name)));
        }
    }

    private static String nameVerdict(String raw) {
        try {
            return "accepted as " + Username.parse(raw).value();
        } catch (Rejected e) {
            return "refused: " + e.rule();
        }
    }

    private static String displayVerdict(String raw) {
        try {
            DisplayName.parse(raw);
            return "accepted";
        } catch (Rejected e) {
            return "refused: " + e.rule();
        }
    }

    /** Counts the bytes a reader actually takes from the stream. */
    private static final class Counting extends FilterInputStream {
        long read;

        Counting(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) {
                read += n;
            }
            return n;
        }
    }

    private static void act4Limits(StringBuilder out) throws IOException {
        line(out, "FOUR. Limits are validation too.");
        line(out, "  THE INPUT, THREE OVERSIZED VALUES:");
        String email = "a".repeat(9_990) + "@shop.example";
        line(out, row("body", "1000000 bytes"));
        line(out, row("cart", "10000 lines"));
        line(out, row("e-mail", email.length() + " characters"));
        line(out, "  THE RESULT, REFUSED BEFORE THE WORK:");
        Counting body = new Counting(new ByteArrayInputStream(new byte[1_000_000]));
        try {
            Limits.body(body);
            line(out, row("body", "accepted"));
        } catch (Rejected e) {
            line(out, row("body", "read " + body.read + ", refused: " + e.rule()));
        }
        try {
            Limits.cart(Collections.nCopies(10_000, "SHO-1042"));
            line(out, row("cart", "accepted"));
        } catch (Rejected e) {
            line(out, row("cart", "refused (" + e.rule() + ")"));
        }
        line(out, row("e-mail", verdict("email", email)));
        Backtracking engine = new Backtracking();
        String small = "aaa!";
        engine.nested(small);
        List<String> splits = engine.splits();
        line(out, "  HOW (a+)+$ READS " + small + " (every split of the a's fails at the !):");
        line(out, "    " + String.join("  ", splits));
        line(out, "    " + splits.size() + " splits = 2^(" + (small.length() - 1) + "-1), " + engine.steps() + " steps in all");
        line(out, "  BEFORE THE FIX, (a+)+$ ON n a's AND A !:");
        long previous = 0;
        for (int n : new int[] {10, 15, 20}) {
            boolean matched = engine.nested("a".repeat(n) + "!");
            String growth = previous == 0 ? "" : String.format(Locale.ROOT, "  (x %.1f)", engine.steps() / (double) previous);
            line(out, row(n + " a's", engine.steps() + " steps, matched " + matched + growth));
            previous = engine.steps();
        }
        line(out, "    5 more a's: about 2^5 = 32 times the work  <- ReDoS");
        line(out, "  AFTER THE FIX, a+$ IN ONE PASS:");
        boolean matched = engine.linear("a".repeat(20) + "!");
        line(out, row("20 a's", engine.steps() + " steps, matched " + matched));
        line(out, "    and a length limit before any pattern runs");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what validation does not do.");
        List<String> names = List.of("<b>great</b>", "O'Brien");
        String[] still = {"still markup: encode it on output", "has a quote: bind it as a parameter"};
        line(out, "  THE INPUT, TWO DISPLAY NAMES:");
        for (String name : names) {
            line(out, "    " + name);
        }
        line(out, "  THE RESULT:");
        for (int i = 0; i < names.size(); i++) {
            line(out, String.format("    %-14s %s, %s", names.get(i), displayVerdict(names.get(i)), still[i]));
        }
        String raw = "ben\r\n<b>x</b>@shop.example";
        line(out, "  THE INPUT, A BAD E-MAIL ADDRESS:");
        line(out, "    " + raw.replace("\r", "\\r").replace("\n", "\\n"));
        try {
            Email.parse(raw);
        } catch (Rejected e) {
            line(out, "  THE RESULT, THE ERROR MESSAGE:");
            line(out, row("message", e.getMessage()));
            line(out, row("repeats it?", e.getMessage().contains("<b>")));
        }
        line(out, "  THE INPUT, A WHOLESALE ORDER:");
        line(out, row("quantity", "150"));
        line(out, "  THE RESULT:");
        line(out, row("quantity", verdict("quantity", "150") + "  <- A CUSTOMER TURNED AWAY"));
        line(out, "  SO:");
        line(out, "    each limit is a written business decision");
        line(out, "    validation narrows what reaches the code;");
        line(out, "    encoding, parameters and access checks still do their jobs");
    }
}
