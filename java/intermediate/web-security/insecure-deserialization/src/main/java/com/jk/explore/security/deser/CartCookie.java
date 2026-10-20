package com.jk.explore.security.deser;

/** Turns a cart into the text of a cookie and back. */
public interface CartCookie {

    String write(Cart cart);

    Cart read(String cookie);
}
