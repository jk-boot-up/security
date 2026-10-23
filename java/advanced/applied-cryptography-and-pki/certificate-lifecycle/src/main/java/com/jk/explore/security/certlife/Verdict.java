package com.jk.explore.security.certlife;

/** Whether a certificate chain was accepted, and the reason in a few words when it was not. */
record Verdict(boolean accepted, String why) {

    static final Verdict OK = new Verdict(true, "accepted");

    String word() {
        return accepted ? "accepted" : "refused (" + why + ")";
    }
}
