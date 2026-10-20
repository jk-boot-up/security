package com.jk.explore.security.deser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * The cookie is the cart, serialized by Java and encoded in Base64. With no filter, {@code readObject} builds <em>any</em> serializable class the
 * cookie names; with an allow-list filter, only the classes a cart is made of.
 */
public final class SerializedCartCookie implements CartCookie {

    /** The classes a cart is made of, and nothing else, with limits on depth, size and number of objects. */
    static final String ALLOW_LIST = "com.jk.explore.security.deser.Cart;com.jk.explore.security.deser.Cart$Line;java.util.ArrayList;"
            + "java.lang.Object;java.lang.String;java.lang.Record;java.lang.Number;java.lang.Long;java.lang.Integer;"
            + "maxdepth=5;maxrefs=100;maxarray=100;maxbytes=2000;!*";

    private final ObjectInputFilter filter;

    private SerializedCartCookie(ObjectInputFilter filter) {
        this.filter = filter;
    }

    /** No filter: the first version. */
    public static SerializedCartCookie unfiltered() {
        return new SerializedCartCookie(null);
    }

    /** The fixed version: a filter with an allow-list. */
    public static SerializedCartCookie withAllowList() {
        return new SerializedCartCookie(ObjectInputFilter.Config.createFilter(ALLOW_LIST));
    }

    @Override
    public String write(Cart cart) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(cart);
            out.flush();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Cart read(String cookie) {
        byte[] data;
        try {
            data = Base64.getUrlDecoder().decode(cookie);
        } catch (IllegalArgumentException e) {
            throw new CartRefused("not Base64");
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            if (filter != null) {
                in.setObjectInputFilter(filter);
            }
            Object o = in.readObject();
            if (o instanceof Cart c) {
                return c;
            }
            throw new CartRefused("the cookie is not a cart"); // too late: the object was already built, and its readObject has already run
        } catch (java.io.InvalidClassException e) {
            throw new CartRefused("class refused by the filter: " + e.getMessage().replaceAll(";.*", ""), e);
        } catch (IOException | ClassNotFoundException e) {
            throw new CartRefused("unreadable cart", e);
        }
    }
}
