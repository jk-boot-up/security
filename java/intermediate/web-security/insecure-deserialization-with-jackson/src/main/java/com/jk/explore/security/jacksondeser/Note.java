package com.jk.explore.security.jacksondeser;

/** A note on a cart. The shop has two kinds; other code on the classpath may implement the interface too, as {@link TempCleaner} does. */
public interface Note {

    record Gift(String message) implements Note {
    }

    record Coupon(String code) implements Note {
    }
}
