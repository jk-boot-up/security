package com.jk.explore.security.deser;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The fixed design: the cookie holds <em>data only</em>, {@code SKU x quantity} pairs as text, and a signature. There are no classes to build, prices
 * are looked up on the server, and a changed cookie fails the signature check before it is parsed.
 */
public final class SignedTextCartCookie implements CartCookie {

    private static final Pattern LINE = Pattern.compile("([A-Z]{3}-[0-9]{3})x([1-9][0-9]?)");

    private final byte[] key;
    private final Prices prices;

    public SignedTextCartCookie(byte[] key, Prices prices) {
        this.key = key.clone();
        this.prices = prices;
    }

    /** The server's own price list, the one source of prices. */
    public interface Prices {
        long cents(String sku);
    }

    @Override
    public String write(Cart cart) {
        StringBuilder text = new StringBuilder("v1");
        for (Cart.Line l : cart.lines()) {
            text.append(';').append(l.sku()).append('x').append(l.quantity());
        }
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toString().getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    @Override
    public Cart read(String cookie) {
        int dot = cookie.indexOf('.');
        if (dot < 0 || cookie.length() > 1000) {
            throw new CartRefused("not a signed cookie");
        }
        String body = cookie.substring(0, dot);
        byte[] expected = sign(body).getBytes(StandardCharsets.US_ASCII);
        byte[] given = cookie.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, given)) { // constant time, and nothing is parsed before this passes
            throw new CartRefused("bad signature");
        }
        String text = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
        String[] parts = text.split(";");
        if (!parts[0].equals("v1") || parts.length > 21) {
            throw new CartRefused("unknown cookie version or too many lines");
        }
        Cart cart = new Cart();
        for (int i = 1; i < parts.length; i++) {
            var m = LINE.matcher(parts[i]);
            if (!m.matches()) {
                throw new CartRefused("bad line");
            }
            cart.add(m.group(1), Integer.parseInt(m.group(2)), prices.cents(m.group(1)));
        }
        return cart;
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
