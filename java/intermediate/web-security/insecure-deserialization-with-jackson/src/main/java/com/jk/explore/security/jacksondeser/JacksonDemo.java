package com.jk.explore.security.jacksondeser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the shopping cart cookie story with Jackson 3 in five numbered acts and prints exact, repeatable results. Each step prints its input
 * (the cookie's JSON) and its result in separate labelled sections, and derived values (the cookie's first characters, totals) are worked out.
 */
public class JacksonDemo {

    static final String PACKAGE = JacksonDemo.class.getPackageName() + ".";
    /** Printed instead of the temporary marker file's random name, so the output is the same on every run. */
    static final String MARKER = "<marker file>";

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        List<Line> lines = List.of(new Line("KTL-001", 2, 2500), new Line("PAN-002", 1, 4200));
        JsonMapper defaults = Mappers.defaults();
        Cart honest = new Cart(lines, new Note.Gift("Happy birthday"));

        line(out, "ONE. A cart in a cookie works for honest customers.");
        line(out, "  THE CART (code, quantity, cents each, a note):");
        for (Line l : lines) {
            line(out, String.format("    %s  x%d  %d", l.sku(), l.qty(), l.price()));
        }
        line(out, "    a Gift note: \"" + ((Note.Gift) honest.gift()).message() + "\"");
        String cookie = Cookies.write(defaults, honest);
        line(out, "  THE COOKIE'S JSON (written by Jackson):");
        json(out, json(cookie));
        line(out, "  THE COOKIE (that JSON in Base64):");
        line(out, "    " + cookie.substring(0, 24) + "...  (" + cookie.length() + " characters)");
        String start = json(cookie).substring(0, 6);
        line(out, "    " + start + "  -> Base64 " + Cookies.fromJson(start) + "  (JSON starts eyJ)");
        Cart back = read(defaults, Cart.class, cookie);
        line(out, "  THE RESULT:");
        line(out, "    " + show(back));
        line(out, "    " + sum(back.lines()));

        line(out, "TWO. The cookie decides which class is built.");
        line(out, "  THE FORGED CART'S JSON (the customer wrote the price):");
        json(out, Attacker.cheapCart());
        line(out, "  THE RESULT:");
        Cart cheap = read(defaults, Cart.class, Cookies.fromJson(Attacker.cheapCart()));
        line(out, "    " + show(cheap) + "  <- A FORGED PRICE");
        line(out, "    " + sum(cheap.lines()));
        line(out, "  THE FORGED NOTE'S JSON (a class name, not a note):");
        json(out, Attacker.classNameNote(MARKER));
        line(out, "  WHAT JACKSON DOES, IN ORDER:");
        line(out, "    1. reads @class and loads " + TempCleaner.class.getSimpleName());
        line(out, "    2. it is a Note, so the default validator allows it");
        line(out, "    3. sets path, and setPath deletes the file");
        line(out, "  THE MARKER FILE BEFORE READING:");
        line(out, "    exists: true");
        line(out, "  THE RESULT:");
        result(out, wipe(defaults, Cart.class, Attacker::classNameNote), "  <- IT RAN");
        line(out, "  OTHER TYPES FOR THE NOTE -> THE RESULT:");
        line(out, "    Object, Jackson 3's defaults");
        line(out, "      -> " + attempt(() -> show(Cookies.read(defaults, LooseCart.class, Cookies.fromJson(
                "{\"lines\":[],\"gift\":{\"@class\":\"" + Note.Gift.class.getName() + "\",\"message\":\"hi\"}}")))));
        line(out, "    Object, default typing on, any class allowed");
        result(out, wipe(Mappers.anyClassMayBeNamed(), Object.class, Attacker::defaultTypingRoot), "  <- IT RAN");

        line(out, "THREE. A list of kinds, or a list of classes.");
        JsonMapper plain = Mappers.defaults();
        TypedCart named = new TypedCart(lines, new Note.Gift("Happy birthday"));
        line(out, "  THE LIST OF KINDS (short names kept in the code):");
        line(out, "    gift -> Note.Gift, coupon -> Note.Coupon");
        String namedCookie = Cookies.write(plain, named);
        line(out, "  THE HONEST NOTE'S JSON:");
        json(out, gift(json(namedCookie)));
        line(out, "    -> " + attempt(() -> show(Cookies.read(plain, TypedCart.class, namedCookie))));
        line(out, "  THE FORGED NOTE'S JSON (a class name as the kind):");
        json(out, gift(Attacker.typeIdNote(MARKER)));
        result(out, wipe(plain, TypedCart.class, Attacker::typeIdNote), "");
        JsonMapper validated = Mappers.onlyOurNotesMayBeNamed();
        line(out, "  THE LIST OF CLASSES (a validator; class names stay):");
        line(out, "    Note.Gift, Note.Coupon");
        line(out, "  THE INPUTS -> THE RESULTS:");
        line(out, "    the honest cart, @class Note$Gift");
        line(out, "      -> " + attempt(() -> show(Cookies.read(validated, Cart.class, Cookies.write(validated, honest)))));
        line(out, "    the forged note, @class " + TempCleaner.class.getSimpleName());
        result(out, wipe(validated, Cart.class, Attacker::classNameNote), "");
        line(out, "    the forged price, price 1, @class Note$Gift");
        line(out, "      -> " + attempt(() -> show(Cookies.read(validated, Cart.class, Cookies.fromJson(Attacker.cheapCart()))))
                + "  <- STILL ACCEPTED");
        line(out, "  a list of types stops unknown classes, not lies");

        line(out, "FOUR. Data only: codes and quantities, priced by the shop.");
        JsonMapper strict = Mappers.strictRequest();
        String order = "{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2},{\"sku\":\"PAN-002\",\"qty\":1}]}";
        line(out, "  THE REQUEST'S JSON (no prices, no class names):");
        json(out, order);
        line(out, "  THE RESULT (each price from the shop's catalogue):");
        line(out, "    " + attempt(() -> priced(out, Cookies.read(strict, OrderRequest.class, Cookies.fromJson(order)))));
        line(out, "  THE FORGED REQUESTS -> THE RESULTS:");
        for (String forged : List.of("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2,\"price\":1}]}",
                "{\"@class\":\"" + TempCleaner.class.getName() + "\",\"items\":[]}",
                "{\"items\":[{\"sku\":\"KTL-001\",\"qty\":-5}]}",
                "{\"items\":[{\"sku\":\"XXX-999\",\"qty\":1}]}")) {
            line(out, "    " + forged.replace(PACKAGE, ""));
            line(out, "      -> " + attempt(() -> priced(null, Cookies.read(strict, OrderRequest.class, Cookies.fromJson(forged)))));
        }

        line(out, "FIVE. Limits on the stream.");
        StreamReadConstraints d = StreamReadConstraints.defaults();
        line(out, "  THE LIMITS (Jackson 3's default -> tightened):");
        line(out, String.format("    nesting depth  %,12d  -> %d", d.getMaxNestingDepth(), 10));
        line(out, String.format("    text length    %,12d  -> %d", d.getMaxStringLength(), 64));
        line(out, String.format("    number length  %,12d  -> %d", d.getMaxNumberLength(), 10));
        line(out, "  THE INPUTS -> THE RESULTS:");
        line(out, "    [[[[ ... ]]]] 5000 levels deep, default limits");
        line(out, "      -> " + attempt(() -> show(Cookies.read(defaults, Object[].class, Cookies.fromJson(Attacker.deepArray(5000))))));
        line(out, "    [[[[ ... ]]]] 11 levels deep, tightened");
        line(out, "      -> " + attempt(() -> show(Cookies.read(strict, Object[].class, Cookies.fromJson(Attacker.deepArray(11))))));
        line(out, "    a 65 character product code, tightened");
        line(out, "      -> " + attempt(() -> priced(null, Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"" + "K".repeat(65) + "\",\"qty\":1}]}")))));
        line(out, "    the quantity 12345678901 (11 digits), tightened");
        line(out, "      -> " + attempt(() -> priced(null, Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":12345678901}]}")))));
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The JSON inside a cookie. */
    private static String json(String cookie) {
        return new String(Base64.getUrlDecoder().decode(cookie), StandardCharsets.UTF_8);
    }

    /** Only the "gift" object of a cart's JSON, to keep a line short. */
    private static String gift(String json) {
        int at = json.indexOf("\"gift\":");
        int depth = 0;
        for (int i = json.indexOf('{', at); i < json.length(); i++) {
            depth += json.charAt(i) == '{' ? 1 : json.charAt(i) == '}' ? -1 : 0;
            if (depth == 0) {
                return json.substring(at, i + 1);
            }
        }
        return json.substring(at);
    }

    /**
     * Prints JSON exactly as it is, broken after a comma or an opening bracket (or, inside a long piece, after a colon) so no line is longer
     * than about 50 characters.
     */
    private static void json(StringBuilder out, String json) {
        List<String> pieces = new ArrayList<>();
        StringBuilder piece = new StringBuilder();
        boolean inString = false;
        for (char c : json.toCharArray()) {
            piece.append(c);
            if (c == '"') {
                inString = !inString;
            } else if (!inString && (c == ',' || c == '[' || c == '{')) {
                pieces.add(piece.toString());
                piece.setLength(0);
            }
        }
        pieces.add(piece.toString());
        StringBuilder current = new StringBuilder();
        for (String whole : pieces) {
            int colon = whole.indexOf("\":") + 2;
            List<String> parts = whole.length() > 46 && colon > 1 ? List.of(whole.substring(0, colon), whole.substring(colon)) : List.of(whole);
            for (String p : parts) {
                if (current.length() + p.length() > 50 && !current.isEmpty()) {
                    line(out, "    " + current);
                    current.setLength(0);
                    current.append("  ");
                }
                current.append(p);
            }
        }
        line(out, "    " + current);
    }

    private static Cart read(JsonMapper mapper, Class<Cart> type, String cookie) {
        try {
            return Cookies.read(mapper, type, cookie);
        } catch (Refused e) {
            throw new IllegalStateException(e);
        }
    }

    private static void result(StringBuilder out, String wiped, String mark) {
        String[] parts = wiped.split("; ");
        line(out, "      -> " + parts[0]);
        line(out, "      -> " + parts[1] + (parts[1].endsWith("false") ? mark : ""));
    }

    private static String show(Cart cart) {
        return cart.lines().size() + (cart.lines().size() == 1 ? " line" : " lines") + ", total " + cart.total() + " cents";
    }

    private static String show(LooseCart cart) {
        return "accepted";
    }

    private static String show(TypedCart cart) {
        return cart.lines().size() + " lines, total " + cart.total() + " cents";
    }

    private static String show(Object[] nested) {
        return "accepted";
    }

    private static String sum(List<Line> lines) {
        StringBuilder s = new StringBuilder();
        long total = 0;
        for (Line l : lines) {
            s.append(s.isEmpty() ? "" : " + ").append(l.qty()).append(" x ").append(l.price());
            total += l.qty() * l.price();
        }
        return s + " = " + total;
    }

    /** The server's own total, from its own prices; the quantity range is checked here. Each line is printed when {@code steps} is given. */
    static String priced(StringBuilder steps, OrderRequest order) throws Refused {
        long total = 0;
        for (OrderRequest.Item item : order.items()) {
            if (item.qty() < 1 || item.qty() > 99) {
                throw new Refused("quantity must be 1 to 99");
            }
            long price = Catalogue.price(item.sku());
            total += item.qty() * price;
            if (steps != null) {
                line(steps, String.format("    %s  x%d  x %d = %d", item.sku(), item.qty(), price, item.qty() * price));
            }
        }
        return order.items().size() + " items, total " + total + " cents";
    }

    private interface Reading {
        String run() throws Refused;
    }

    private static String attempt(Reading reading) {
        try {
            return reading.run();
        } catch (Refused e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private interface Crafted {
        String cookieFor(String path);
    }

    /** Reads the cookie of a TempCleaner that points at a marker file, and reports whether reading it removed the file. */
    private static <T> String wipe(JsonMapper mapper, Class<T> type, Crafted crafted) throws IOException {
        Path marker = Files.createTempFile("cart-marker", ".txt");
        try {
            String result;
            try {
                Cookies.read(mapper, type, Cookies.fromJson(crafted.cookieFor(marker.toString())));
                result = "accepted as a note";
            } catch (Refused e) {
                result = "refused (" + e.getMessage() + ")";
            }
            return result + "; marker file exists afterwards: " + Files.exists(marker);
        } finally {
            Files.deleteIfExists(marker);
        }
    }
}
