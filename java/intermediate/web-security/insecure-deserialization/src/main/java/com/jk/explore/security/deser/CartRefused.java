package com.jk.explore.security.deser;

/** The cookie was refused. The reason is for the security log; the customer just gets an empty cart. */
public class CartRefused extends RuntimeException {

    public CartRefused(String reason) {
        super(reason);
    }

    public CartRefused(String reason, Throwable cause) {
        super(reason, cause);
    }
}
