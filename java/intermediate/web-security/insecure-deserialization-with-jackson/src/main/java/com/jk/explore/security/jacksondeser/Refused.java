package com.jk.explore.security.jacksondeser;

/** The cookie was refused, with a short reason that is the same on every run. */
public final class Refused extends Exception {

    public Refused(String reason) {
        super(reason);
    }
}
