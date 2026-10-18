package com.jk.explore.security.signing;

/** The answer to "may this artifact be deployed?", with the reason. */
record Verdict(boolean accepted, String reason) {

    static Verdict accept(String reason) {
        return new Verdict(true, reason);
    }

    static Verdict refuse(String reason) {
        return new Verdict(false, reason);
    }

    @Override
    public String toString() {
        return (accepted ? "accepted" : "refused") + " (" + reason + ")";
    }
}
