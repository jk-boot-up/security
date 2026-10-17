package com.jk.explore.security.validation;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static void act1Boundary(StringBuilder out) {
        line(out, "ONE. Where validation belongs.");
        Map<String, String> negative = Map.of("sku", "SHO-1042", "quantity", "-3", "email", "ana@shop.example", "country", "IN");
        line(out, "  the checkout page refuses a quantity of -3 before sending: " + !BrowserForm.wouldSubmit(negative));
        line(out, "  the same request sent without the page, to a shop that trusts the page: total " + TrustingShop.total(negative)
                + "  <- A NEGATIVE TOTAL IS A REFUND");
        Map<String, String> cheap = Map.of("sku", "SHO-1042", "quantity", "2", "price", "0.01", "email", "ana@shop.example", "country", "IN");
        line(out, "  a hidden price field changed to 0.01: total " + TrustingShop.total(cheap) + " instead of 179.80  <- THE CUSTOMER SET THE PRICE");
        Checkout.Result refused = Checkout.place(negative);
        line(out, "  the checkout that validates on the server: accepted " + refused.accepted() + " (" + problems(refused) + ")");
        Checkout.Result priced = Checkout.place(cheap);
        line(out, "  and the hidden price: accepted " + priced.accepted() + " (" + problems(priced) + ")");
        Checkout.Result ok = Checkout.place(honest());
        line(out, "  an honest order: accepted " + ok.accepted() + ", total " + ok.order().total() + " from the catalogue");
    }

    private static void act2AllowLists(StringBuilder out) {
        line(out, "TWO. Say what is allowed, and parse into types.");
        String[][] cases = {
            {"quantity", "7"}, {"quantity", "0"}, {"quantity", "1e2"}, {"quantity", " 5"},
            {"sku", "SHO-1042"}, {"sku", "sho-1042"}, {"sku", "SHO-1042;"},
            {"email", "Ben@Shop.EXAMPLE"}, {"email", "ben@@shop.example"}, {"email", "ben@localhost"},
            {"country", "DE"}, {"country", "ZZ"}, {"country", "de"},
            {"amount", "19.50"}, {"amount", "19.505"}, {"amount", "-5"}, {"amount", "NaN"},
        };
        for (String[] c : cases) {
            line(out, "  " + c[0] + " \"" + c[1] + "\": " + verdict(c[0], c[1]));
        }
        Checkout.Result all = Checkout.place(Map.of("sku", "shoes", "quantity", "lots", "email", "x", "country", "Mars"));
        line(out, "  a form with four bad fields reports all four at once: " + all.problems().size());
        line(out, "  each value that passes is a type that cannot hold anything else, so later code never checks it again");
    }

    private static String verdict(String field, String raw) {
        try {
            Object value = switch (field) {
                case "quantity" -> Quantity.parse(raw).value();
                case "sku" -> Sku.parse(raw).value();
                case "email" -> Email.parse(raw).value();
                case "country" -> CountryCode.parse(raw).value();
                default -> Money.parse(raw).amount().toPlainString();
            };
            return "accepted as " + value;
        } catch (Rejected e) {
            return "refused (" + e.rule() + ")";
        }
    }

    private static void act3Canonical(StringBuilder out) {
        line(out, "THREE. Canonicalise before validating.");
        Accounts accounts = new Accounts();
        accounts.registerNaively("ana");
        line(out, "  ana is registered; a registry that compares raw text also accepts the full-width ａｎａ: "
                + accounts.registerNaively("ａｎａ") + "  <- TWO ACCOUNTS FOR ONE NAME");
        line(out, "  and ANA in capitals: " + accounts.registerNaively("ANA"));
        try {
            accounts.register("ana");
            line(out, "  the canonical registry takes ana: true");
            accounts.register("ａｎａ");
            line(out, "  and the full-width spelling: true");
        } catch (Rejected e) {
            line(out, "  the canonical registry folds the full-width spelling to ana: refused (" + e.rule() + ")");
        }
        line(out, "  an invisible zero-width space inside a name: " + nameVerdict("an​a"));
        line(out, "  a line break inside a name: " + nameVerdict("ana\nadmin"));
        line(out, "  Cyrillic а in place of a: " + nameVerdict("аna") + "; login names are kept to plain letters");
        String precomposed = "José";
        String decomposed = "José";
        line(out, "  José typed two ways looks the same; as raw text equal: " + precomposed.equals(decomposed)
                + ", after NFC: " + Text.nfc(precomposed).equals(Text.nfc(decomposed)));
        line(out, "  a display name \"Shоp Suppоrt\" with Cyrillic o: " + displayVerdict("Shоp Suppоrt")
                + "; \"José Martín\": " + displayVerdict("José Martín"));
    }

    private static String nameVerdict(String raw) {
        try {
            return "accepted as " + Username.parse(raw).value();
        } catch (Rejected e) {
            return "refused (" + e.rule() + ")";
        }
    }

    private static String displayVerdict(String raw) {
        try {
            DisplayName.parse(raw);
            return "accepted";
        } catch (Rejected e) {
            return "refused (" + e.rule() + ")";
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
        Counting body = new Counting(new ByteArrayInputStream(new byte[1_000_000]));
        try {
            Limits.body(body);
            line(out, "  a request body of 1000000 bytes: accepted");
        } catch (Rejected e) {
            line(out, "  a request body of 1000000 bytes: refused (" + e.rule() + ") after reading " + body.read + " of them");
        }
        try {
            Limits.cart(Collections.nCopies(10_000, "SHO-1042"));
            line(out, "  a cart of 10000 lines: accepted");
        } catch (Rejected e) {
            line(out, "  a cart of 10000 lines: refused (" + e.rule() + ")");
        }
        line(out, "  an e-mail address of 10000 characters: " + verdict("email", "a".repeat(9_990) + "@shop.example"));
        Backtracking engine = new Backtracking();
        for (int n : new int[] {10, 15, 20}) {
            boolean matched = engine.nested("a".repeat(n) + "!");
            line(out, "  the pattern (a+)+$ on " + n + " a's and a !: matched " + matched + " after " + engine.steps() + " steps");
        }
        line(out, "  each extra character roughly doubles the work  <- A FEW DOZEN CHARACTERS CAN STOP A SERVER (ReDoS)");
        engine.linear("a".repeat(20) + "!");
        line(out, "  the same rule written without nesting, a+$, checked in one pass: " + engine.steps() + " steps for 20 a's and a !");
        line(out, "  so: no nested repetition in patterns that read input, and a length limit before any pattern runs");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what validation does not do.");
        line(out, "  a review \"<b>great</b>\" is valid text, and still markup: valid is not the same as safe to put in a page,"
                + " so encode it on output (see cross-site-scripting)");
        line(out, "  a surname O'Brien is valid and contains a quote, so the database layer must bind values as parameters,"
                + " never paste them into query text");
        try {
            Email.parse("ben\r\n<b>x</b>@shop.example");
        } catch (Rejected e) {
            line(out, "  a refused e-mail address with a line break and markup: \"" + e.getMessage() + "\", raw input repeated: "
                    + e.getMessage().contains("<b>"));
        }
        line(out, "  a wholesale order of 150: " + verdict("quantity", "150")
                + "; a limit stricter than the business turns customers away, so each rule is a written business decision");
        line(out, "  validation narrows what reaches the code; encoding, parameters and access checks still do their own jobs");
    }
}
