package com.jk.explore.security.deser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/** Runs the shopping cart cookie story in four numbered acts and prints exact, repeatable results. */
public class DeserializationDemo {

    static final byte[] DEMO_KEY = "demo-signing-key-not-for-production".getBytes(StandardCharsets.UTF_8);
    static final SignedTextCartCookie.Prices PRICES = sku -> sku.equals("KTL-001") ? 2500 : 4200;

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        Cart honest = new Cart().add("KTL-001", 2, 2500).add("PAN-002", 1, 4200);

        out.append("== 1. A cart in a cookie ==\n");
        CartCookie plain = SerializedCartCookie.unfiltered();
        out.append("cart round trip              -> ").append(show(plain.read(plain.write(honest)))).append('\n');

        out.append("\n== 2. The cookie decides what the server builds ==\n");
        Cart cheap = new Cart().add("KTL-001", 2, 1);
        out.append("forged price                 -> ").append(show(plain.read(Attacker.cookieFor(cheap)))).append('\n');
        out.append("cookie holds a TempCleaner   -> ").append(wipe(plain)).append('\n');

        out.append("\n== 3. An allow-list filter names the classes a cart is made of ==\n");
        CartCookie filtered = SerializedCartCookie.withAllowList();
        out.append("cart round trip              -> ").append(show(filtered.read(filtered.write(honest)))).append('\n');
        out.append("cookie holds a TempCleaner   -> ").append(wipe(filtered)).append('\n');
        out.append("forged price                 -> ").append(show(filtered.read(Attacker.cookieFor(cheap)))).append("  <- STILL ACCEPTED\n");

        out.append("\n== 4. Data only: names and quantities, signed ==\n");
        CartCookie signed = new SignedTextCartCookie(DEMO_KEY, PRICES);
        String cookie = signed.write(honest);
        out.append("cart round trip              -> ").append(show(signed.read(cookie))).append('\n');
        out.append("a serialized object          -> ").append(refused(signed, Attacker.cookieFor(cheap))).append('\n');
        out.append("quantity changed             -> ").append(refused(signed, tamper(cookie))).append('\n');
        out.append("unsigned text                -> ").append(refused(signed, Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1;KTL-001x2".getBytes(StandardCharsets.UTF_8)) + ".AAAA")).append('\n');
        return out.toString();
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
                result = "refused (" + e.getMessage() + ")";
            }
            return result + "; marker file exists afterwards: " + Files.exists(marker);
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    static String tamper(String cookie) {
        int dot = cookie.indexOf('.');
        String text = new String(Base64.getUrlDecoder().decode(cookie.substring(0, dot)), StandardCharsets.UTF_8).replace("x2", "x9");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + cookie.substring(dot);
    }

    private static String show(Cart c) {
        return c.lines().size() + " lines, total " + c.totalCents() + " cents";
    }

    private static String refused(CartCookie reader, String cookie) {
        try {
            return "accepted: " + show(reader.read(cookie));
        } catch (CartRefused e) {
            return "refused: " + e.getMessage();
        }
    }
}
