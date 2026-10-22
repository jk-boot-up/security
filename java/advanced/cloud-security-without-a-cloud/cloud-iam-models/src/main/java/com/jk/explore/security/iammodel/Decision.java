package com.jk.explore.security.iammodel;

/** The answer, and the layer or rule that decided it. */
public record Decision(boolean allowed, String why) {

    @Override
    public String toString() {
        return (allowed ? "ALLOW" : "DENY ") + " (" + why + ")";
    }
}
