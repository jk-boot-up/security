package com.jk.explore.security.deser;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the shopping cart cookie story in four numbered acts and prints exact, repeatable results. Each step prints its input and its result in
 * separate labelled sections, and the derived values (the cookie's first bytes, the classes it names, the total, the signature) are worked out.
 */
public class DeserializationDemo {

    static final byte[] DEMO_KEY = "demo-signing-key-not-for-production".getBytes(StandardCharsets.UTF_8);
    static final SignedTextCartCookie.Prices PRICES = sku -> sku.equals("KTL-001") ? 2500 : 4200;

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        Cart honest = new Cart().add("KTL-001", 2, 2500).add("PAN-002", 1, 4200);
        Cart cheap = new Cart().add("KTL-001", 2, 1);
        CartCookie plain = SerializedCartCookie.unfiltered();

        line(out, "ONE. A cart in a cookie works for honest customers.");
        line(out, "  THE CART (code, quantity, price per unit in cents):");
        cartLines(out, honest);
        String cookie = plain.write(honest);
        byte[] bytes = Base64.getUrlDecoder().decode(cookie);
        line(out, "  THE COOKIE (the cart serialized by Java, then Base64):");
        line(out, "    " + cookie.substring(0, 24) + "...  (" + cookie.length() + " characters)");
        line(out, "  HOW IT STARTS (the first six bytes, in hex):");
        line(out, "    " + HexFormat.ofDelimiter(" ").formatHex(bytes, 0, 6).toUpperCase() + "  -> Base64 " + cookie.substring(0, 8));
        line(out, "    AC ED = the serialization magic, 00 05 = version 5");
        line(out, "    73 = a new object, 72 = its class description");
        line(out, "  THE CLASSES THE COOKIE NAMES (each one is built):");
        List<String> named = new ArrayList<>();
        Cart back = SerializedCartCookie.withFilter(recording(null, named, false)).read(cookie);
        classes(out, named);
        line(out, "  THE RESULT:");
        line(out, "    " + show(back));
        line(out, "    " + sum(back));

        line(out, "TWO. The cookie decides what the server builds.");
        line(out, "  THE FORGED CART (written by the customer):");
        cartLines(out, cheap);
        line(out, "  THE RESULT:");
        Cart believed = plain.read(Attacker.cookieFor(cheap));
        line(out, "    " + show(believed) + "  <- A FORGED PRICE");
        line(out, "    " + sum(believed));
        line(out, "  THE FORGED COOKIE (not a cart at all):");
        line(out, "    a TempCleaner, path = <a marker file on the server>");
        List<String> gadget = new ArrayList<>();
        line(out, "  THE MARKER FILE BEFORE READING:");
        line(out, "    exists: true");
        String wiped = wipe(SerializedCartCookie.withFilter(recording(null, gadget, false)));
        line(out, "  THE CLASSES THE COOKIE NAMES (each one is built):");
        classes(out, gadget);
        line(out, "  THE RESULT:");
        line(out, "    " + wiped.replace("; marker", "\n    marker") + "  <- IT ALREADY RAN");
        line(out, "  checking the type after readObject is too late");

        line(out, "THREE. An allow-list filter names the classes of a cart.");
        line(out, "  THE ALLOW-LIST (checked before each class is built):");
        allowList(out);
        line(out, "  THE HONEST CART -> THE FILTER'S ANSWERS:");
        List<String> answers = new ArrayList<>();
        Cart again = SerializedCartCookie.withFilter(recording(SerializedCartCookie.allowList(), answers, true)).read(cookie);
        classes(out, answers);
        line(out, "    -> " + show(again));
        line(out, "  THE TEMPCLEANER COOKIE -> THE FILTER'S ANSWER:");
        List<String> refused = new ArrayList<>();
        String kept = wipe(SerializedCartCookie.withFilter(recording(SerializedCartCookie.allowList(), refused, true)));
        classes(out, refused);
        line(out, "  THE RESULT:");
        line(out, "    " + kept.replace(": filter status: REJECTED", "").replace("; marker", "\n    marker"));
        line(out, "  THE FORGED PRICE -> THE FILTER'S ANSWERS:");
        List<String> forged = new ArrayList<>();
        Cart lie = SerializedCartCookie.withFilter(recording(SerializedCartCookie.allowList(), forged, false)).read(Attacker.cookieFor(cheap));
        line(out, "    " + String.join(", ", forged) + " -> all ALLOWED");
        line(out, "    -> " + show(lie) + "  <- STILL ACCEPTED");
        line(out, "  a filter stops unknown classes, not lies");

        line(out, "FOUR. Data only: product codes and quantities, signed.");
        SignedTextCartCookie signed = new SignedTextCartCookie(DEMO_KEY, PRICES);
        String good = signed.write(honest);
        int dot = good.indexOf('.');
        line(out, "  THE COOKIE TEXT (no classes, no prices):");
        line(out, "    " + text(good));
        line(out, "  THE BODY (the text in Base64):");
        line(out, "    " + good.substring(0, dot));
        line(out, "  THE SIGNATURE (HMAC-SHA-256 of the body, server's key):");
        line(out, "    " + good.substring(dot + 1));
        line(out, "  THE COOKIE IS body.signature");
        line(out, "  THE RESULT (signature checked first, then prices looked up):");
        Cart read = signed.read(good);
        for (Cart.Line l : read.lines()) {
            line(out, String.format("    %s  x%d  x %d = %d", l.sku(), l.quantity(), l.priceCents(), l.quantity() * l.priceCents()));
        }
        line(out, "    " + show(read));
        line(out, "  THE QUANTITY CHANGED BY THE CUSTOMER (x2 -> x9):");
        String changed = tamper(good);
        line(out, "    " + text(changed) + "  (the new text)");
        line(out, "    the server's HMAC of the new body:");
        line(out, "      " + signed.signatureOf(changed.substring(0, changed.indexOf('.'))));
        line(out, "    the signature in the cookie (unchanged):");
        line(out, "      " + changed.substring(changed.indexOf('.') + 1));
        line(out, "    -> " + refused(signed, changed));
        line(out, "  THE OTHER FORGED COOKIES:");
        line(out, "    a serialized object -> " + refused(signed, Attacker.cookieFor(cheap)));
        line(out, "    unsigned text       -> " + refused(signed, Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1;KTL-001x2".getBytes(StandardCharsets.UTF_8)) + ".AAAA"));
        return out.toString();
    }

    /**
     * A filter that asks {@code inner} (or allows everything when it is null) and records the short name of every class the stream names,
     * with the answer when {@code answers} is true. It changes nothing; it only makes the filter's work visible.
     */
    static ObjectInputFilter recording(ObjectInputFilter inner, List<String> log, boolean answers) {
        Set<String> seen = new LinkedHashSet<>();
        return info -> {
            ObjectInputFilter.Status status = inner == null ? ObjectInputFilter.Status.UNDECIDED : inner.checkInput(info);
            Class<?> c = info.serialClass();
            if (c != null && seen.add(c.getName())) {
                String name = c.getName().replace("com.jk.explore.security.deser.", "").replace("java.util.", "").replace("java.lang.", "")
                        .replace("[LObject;", "Object[]");
                log.add(answers ? String.format("%-10s -> %s", name, status) : name);
            }
            return status;
        };
    }

    /** Builds the cookie of a TempCleaner that points at a marker file, and reports whether reading the cookie removed the file. */
    static String wipe(CartCookie reader) throws IOException {
        Path marker = Files.createTempFile("cart-marker", ".txt");
        try {
            String result;
            try {
                reader.read(Attacker.cookieFor(new TempCleaner(marker.toString())));
                result = "accepted";
            } catch (CartRefused e) {
                result = "refused: " + e.getMessage();
            }
            return result + "; marker file exists afterwards: " + Files.exists(marker);
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    static String tamper(String cookie) {
        int dot = cookie.indexOf('.');
        String text = text(cookie).replace("x2", "x9");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + cookie.substring(dot);
    }

    private static String text(String cookie) {
        return new String(Base64.getUrlDecoder().decode(cookie.substring(0, cookie.indexOf('.'))), StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void cartLines(StringBuilder out, Cart cart) {
        for (Cart.Line l : cart.lines()) {
            line(out, String.format("    %s  x%d  %d", l.sku(), l.quantity(), l.priceCents()));
        }
    }

    private static void classes(StringBuilder out, List<String> names) {
        if (names.stream().anyMatch(n -> n.contains("->"))) {
            names.forEach(n -> line(out, "    " + n));
        } else {
            line(out, "    " + String.join(", ", names));
        }
    }

    private static void allowList(StringBuilder out) {
        String[] parts = SerializedCartCookie.ALLOW_LIST.replace("com.jk.explore.security.deser.", "").replace("java.util.", "")
                .replace("java.lang.", "").split(";");
        List<String> names = new ArrayList<>();
        List<String> limits = new ArrayList<>();
        for (String p : parts) {
            (p.contains("=") || p.equals("!*") ? limits : names).add(p);
        }
        line(out, "    " + String.join(" ", names.subList(0, 5)));
        line(out, "    " + String.join(" ", names.subList(5, names.size())));
        line(out, "    " + String.join(" ", limits.subList(0, 2)));
        line(out, "    " + String.join(" ", limits.subList(2, limits.size())) + "  (!* = refuse the rest)");
    }

    private static String show(Cart c) {
        return c.lines().size() + (c.lines().size() == 1 ? " line" : " lines") + ", total " + c.totalCents() + " cents";
    }

    private static String sum(Cart c) {
        StringBuilder s = new StringBuilder();
        for (Cart.Line l : c.lines()) {
            s.append(s.isEmpty() ? "" : " + ").append(l.quantity()).append(" x ").append(l.priceCents());
        }
        return s + " = " + c.totalCents();
    }

    private static String refused(CartCookie reader, String cookie) {
        try {
            return "accepted: " + show(reader.read(cookie));
        } catch (CartRefused e) {
            return "refused: " + e.getMessage();
        }
    }
}
