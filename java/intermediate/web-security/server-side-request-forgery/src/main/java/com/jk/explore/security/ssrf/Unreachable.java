package com.jk.explore.security.ssrf;

/** No host with that name, or nothing listening at that address. */
public class Unreachable extends RuntimeException {

    public Unreachable(String message) {
        super(message);
    }
}
