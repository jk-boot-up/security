package com.jk.explore.security.jacksondeser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Runs the shopping cart cookie story with Jackson 3 in five numbered acts and prints exact, repeatable results. */
public class JacksonDemo {

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        List<Line> lines = List.of(new Line("KTL-001", 2, 2500), new Line("PAN-002", 1, 4200));

        out.append("== 1. A cart in a cookie ==\n");
        JsonMapper defaults = Mappers.defaults();
        Cart honest = new Cart(lines, new Note.Gift("Happy birthday"));
        row(out, "cart round trip", attempt(() -> show(Cookies.read(defaults, Cart.class, Cookies.write(defaults, honest)))));

        out.append("\n== 2. The cookie decides which class is built ==\n");
        row(out, "an Object field", attempt(() -> show(Cookies.read(defaults, LooseCart.class, Cookies.fromJson(
                "{\"lines\":[],\"gift\":{\"@class\":\"" + Note.Gift.class.getName() + "\",\"message\":\"hi\"}}")))));
        row(out, "forged price", attempt(() -> show(Cookies.read(defaults, Cart.class, Cookies.fromJson(Attacker.cheapCart())))));
        row(out, "cookie holds a TempCleaner", wipe(defaults, Cart.class, Attacker::classNameNote));
        row(out, "global default typing, TempCleaner", wipe(Mappers.anyClassMayBeNamed(), Object.class, Attacker::defaultTypingRoot));

        out.append("\n== 3. A list of kinds, or a list of classes ==\n");
        JsonMapper plain = Mappers.defaults();
        TypedCart named = new TypedCart(lines, new Note.Gift("Happy birthday"));
        row(out, "names: cart round trip", attempt(() -> show(Cookies.read(plain, TypedCart.class, Cookies.write(plain, named)))));
        row(out, "names: cookie holds a TempCleaner", wipe(plain, TypedCart.class, Attacker::typeIdNote));
        JsonMapper validated = Mappers.onlyOurNotesMayBeNamed();
        row(out, "validator: cart round trip", attempt(() -> show(Cookies.read(validated, Cart.class, Cookies.write(validated, honest)))));
        row(out, "validator: cookie holds a TempCleaner", wipe(validated, Cart.class, Attacker::classNameNote));
        row(out, "forged price", attempt(() -> show(Cookies.read(validated, Cart.class, Cookies.fromJson(Attacker.cheapCart()))))
                + "  <- STILL ACCEPTED");

        out.append("\n== 4. Data only: names and quantities, priced by the shop ==\n");
        JsonMapper strict = Mappers.strictRequest();
        row(out, "order round trip", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2},{\"sku\":\"PAN-002\",\"qty\":1}]}")))));
        row(out, "forged price", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2,\"price\":1}]}")))));
        row(out, "a class name", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"@class\":\"" + TempCleaner.class.getName() + "\",\"items\":[]}")))));
        row(out, "quantity -5", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":-5}]}")))));
        row(out, "unknown product", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"XXX-999\",\"qty\":1}]}")))));

        out.append("\n== 5. Limits on the stream ==\n");
        row(out, "default limits, 5000 levels deep", attempt(() -> show(Cookies.read(defaults, Object[].class,
                Cookies.fromJson(Attacker.deepArray(5000))))));
        row(out, "tightened, 11 levels deep", attempt(() -> show(Cookies.read(strict, Object[].class, Cookies.fromJson(Attacker.deepArray(11))))));
        row(out, "tightened, a 65 character product", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"" + "K".repeat(65) + "\",\"qty\":1}]}")))));
        row(out, "tightened, an 11 digit quantity", attempt(() -> priced(Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":12345678901}]}")))));
        return out.toString();
    }

    private static void row(StringBuilder out, String label, String result) {
        out.append(String.format("%-37s -> %s%n", label, result));
    }

    private static String show(Cart cart) {
        return cart.lines().size() + " lines, total " + cart.total() + " cents";
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

    /** The server's own total, from its own prices; the quantity range is checked here. */
    private static String priced(OrderRequest order) throws Refused {
        long total = 0;
        for (OrderRequest.Item item : order.items()) {
            if (item.qty() < 1 || item.qty() > 99) {
                throw new Refused("quantity must be 1 to 99");
            }
            total += item.qty() * Catalogue.price(item.sku());
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
